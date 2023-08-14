package com.furiousTidy.magicbean.trader.service;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.client.model.market.Trade;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.trader.TradeDto.JinShiDto;
import com.furiousTidy.magicbean.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;
import static com.furiousTidy.magicbean.util.TradeUtil.getCurrentTime;


/*
* 根据新上合约冲现货策略
* 机器人太多，有些现货插针，很可能买在最高点，放弃
* */
@Component
@Slf4j

public class NewPerpToSpotService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    Perp2SpotUtil perp2SpotUtil;

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    TradeUtil tradeUtil;

    static String url = "https://www.binance.com/zh-CN/support/announcement/%E6%95%B0%E5%AD%97%E8%B4%A7%E5%B8%81%E5%8F%8A%E4%BA%A4%E6%98%93%E5%AF%B9%E4%B8%8A%E6%96%B0?c=48&navId=48";

    @Async
    public void doSpot() throws InterruptedException {

        while (true) {
            List<String> perpNameList = new ArrayList<>();
            try {
                perpNameList= getPerpName();
            }catch (Exception e){
                log.error("get perp name error, exception={}",e);
                Thread.sleep(60000);
            }

            for(String perpName: perpNameList){
                String symbol = perpName + "USDT";
                if(perp2SpotUtil.containPerpName(symbol)){
                    continue;
                }

                //spot 开多
                perp2SpotUtil.savePerpName(symbol);
                log.info("发现新上合约，开多现货");

                NewOrderResponse newOrderResponse = null;
                String clientOrderId = symbol +"_"+ getCurrentTime();
                try{
                    newOrderResponse = openNewSpotOrder(symbol,clientOrderId);
                }catch (Exception e){
                    if(e.getMessage().contains("insufficient balance")){
                        log.error("spot insufficient money exception......id={},exception={}",clientOrderId,e);
                        BeanConstant.ENOUGH_MONEY.set(false);
                    }else{
                        log.error("spot order exception...id={},exception={}",clientOrderId,e);
                    }
                }

                if(newOrderResponse == null){
                    log.error("spot is insufficient null......id={}",clientOrderId);
                    BeanConstant.ENOUGH_MONEY.set(false);
                    return;
                }
                log.info("new spot order return, clientid={},price={},qty={},order={}", newOrderResponse.getClientOrderId()
                        ,newOrderResponse.getFills().get(0).getPrice(),newOrderResponse.getExecutedQty(),newOrderResponse);

                if(newOrderResponse.getStatus() == OrderStatus.FILLED){
                    new PriceMonitor(newOrderResponse).run();
                }
            }
            Thread.sleep(60);
        }
    }

    class PriceMonitor implements Runnable{

        NewOrderResponse orderResponse;

        public PriceMonitor(NewOrderResponse newOrderResponse){
            orderResponse = newOrderResponse;
        }

        @Override
        public void run() {
            log.info("perp2spot现货价格监控运行...........");
            BigDecimal maxPrice = BigDecimal.ZERO;
            while(true){
                String symbol = orderResponse.getSymbol();
                BigDecimal basePrice = new BigDecimal( orderResponse.getFills().get(0).getPrice());
                BigDecimal currentPrice = MarketCache.spotTickerMap.get(symbol).getBidPrice();
                //记录最高价
                if(currentPrice.compareTo(maxPrice) > 0){
                    maxPrice = currentPrice;
                }else {
                    //下跌超过最高点10%，平仓
                    BigDecimal plusPrice = maxPrice.subtract(currentPrice).multiply(new BigDecimal("0.9"));
                    if(basePrice.add(plusPrice).compareTo(currentPrice) > 1){
                        NewOrderResponse closeOrderResponse = spotSyncClientProxy
                                .newOrder(marketSell(symbol, orderResponse.getFills().get(0).getQty())
                                        .newOrderRespType(NewOrderResponseType.FULL)
                                        .newClientOrderId(orderResponse.getClientOrderId()));
                        //下单完毕，中断当前线程
                        Thread.currentThread().interrupt();
                    }
                }

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private NewOrderResponse openNewSpotOrder(String symbol, String clientOrderId) throws Exception{
        BigDecimal balance =  MarketCache.spotBalance.get();
        BookTickerModel bookTickerModel = MarketCache.spotTickerMap.get(symbol);
        Integer[] stepSize = tradeUtil.getStepSize(symbol);
        BigDecimal spotQty = balance.divide(bookTickerModel.getAskPrice(), stepSize[1], RoundingMode.HALF_UP);
        log.info("perp2spot 开多现货,symbol={},balance={}, qty={}",symbol, balance,spotQty);
        return  spotSyncClientProxy.newOrder(marketBuy(symbol, spotQty.toString()).newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
    }



    //解析返回值，获得perp name，查找是否在合约列表里，如果没有则直接现货买入
    //幣安將上線Threshold（T） 1-20倍 U本位永續合約2023-01-31
    //幣安將上線1000LUNC 1-25倍 U本位永續合約
    //幣安將上線SPELL 1-25倍 U本位永續合約
    //幣安將上線AUCTION和UNI 1-20倍 BUSD永續合約
    //幣安將上線FIL、1000SHIB和LEVER 1-20倍 BUSD永續合約
    List<String> getPerpName() throws Exception{
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity entity = new HttpEntity(headers);

        List<String> perpNameList = new ArrayList<>();

        String reponse = "";

        try{
            reponse = restTemplate.exchange(url, HttpMethod.GET,entity, String.class).getBody();
        }catch (Exception ex){
            log.error("get url exception: {}", ex);
        }

        if(reponse.contains("U本位永續合約")){
           int perpIndex = reponse.indexOf("U本位永續合約");
           reponse = reponse.substring(0,perpIndex);
           int bnPrepIndex = reponse.lastIndexOf("幣安將上線");
           reponse = reponse.substring(bnPrepIndex);
           int spaceIndex = reponse.indexOf(" ");
           String perpNameStr = reponse.substring(5, spaceIndex);

           perpNameList = reprogramStr(perpNameStr);

        }
        return perpNameList;
    }

    List reprogramStr( String perpNameStr){
        List perpNameList = new ArrayList();

        String[] perpNameStrDot =  perpNameStr.split("、");

        for(String perpName2: perpNameStrDot) {

            String[] perpNameStrHe = perpName2.split("和");
            for (String perpName3 : perpNameStrHe) {
                perpNameList.add(executeString1000(perpName3));
            }
        }
        return perpNameList;
    }
    String executeString1000(String perpName3) {
        if(perpName3.contains("1000")){
            return perpName3.substring(4);
        }else if(perpName3.contains("（")){
            return executeStringBracket(perpName3);
        }else {
            return perpName3;
        }
    }
    //Threshold（T） 1-20倍 U本位永續合約
    String executeStringBracket(String perpName4){
            int index1 = perpName4.indexOf("（");
            int index2 = perpName4.indexOf("）");
            return perpName4.substring(index1+1, index2);
    }



    public static void main(String[] args){
        String[] testStr = {"幣安將上線Threshold（T） 1-20倍 U本位永續合約", "幣安將上線Threshold（T） 1-20倍 U本位永續合約2023-01-31","123456幣安將上線1000LUNC 1-25倍 U本位永續合約1234556","fdfdwew幣安將上線SPELL 1-25倍 U本位永續合約afdaerew",
         "幣安將上線APT和QNT 1-25倍 U本位永續合約", "37654321 幣安將上線FIL、1000SHIB和LEVER 1-20倍 U本位永續合約 akjklkassd"};

        for( String bnStr: testStr){
            List perpNameList = new ArrayList();
            int perpIndex = bnStr.indexOf("U本位永續合約");

            bnStr = bnStr.substring(0,perpIndex);
            int bnPrepIndex = bnStr.lastIndexOf("幣安將上線");
            bnStr = bnStr.substring(bnPrepIndex);
            int spaceIndex = bnStr.indexOf(" ");
            String perpNameStr = bnStr.substring(5, spaceIndex);

            NewPerpToSpotService newPerpToSpotService = new NewPerpToSpotService();
            perpNameList = newPerpToSpotService.reprogramStr(perpNameStr);

            System.out.println("perpNameList = " + perpNameList);
        }


    }

}
