package com.furiousTidy.magicbean.trader;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.client.RequestOptions;
import com.binance.client.SyncRequestClient;
import com.binance.client.exception.BinanceApiException;
import com.binance.client.model.enums.*;
import com.binance.client.model.event.MarkPriceEvent;
import com.binance.client.model.market.MarkPrice;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.Subscription.FutureSubscription;
import com.furiousTidy.magicbean.Subscription.SpotSubscription;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sun.jvm.hotspot.oops.Mark;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;


/*
*  开仓
*
* */
@Service
public class PositionOpenService {
    static Logger logger = LoggerFactory.getLogger(PositionOpenService.class);


    //处理资金费率
    public void processFundingRate(){
        MarketCache.markPriceList = BinanceClient.futureSyncClient.getMarkPrice(null);
        Collections.sort( MarketCache.markPriceList, new Comparator<MarkPrice>() {
            public int compare(MarkPrice o1, MarkPrice o2) {
                return o2.getLastFundingRate().compareTo(o1.getLastFundingRate());
            }
        });
        for(MarkPrice markPrice : MarketCache.markPriceList){
            logger.info(markPrice.getLastFundingRate().toString()+":"+markPrice.getSymbol());
        }
    }


    //处理list，获取资金费率最高的深度行情
    public void moneyProcess(List<MarkPriceEvent> markPrices){

        //获取当前账户资金


        //资金费率最高的标的
        String symbol = markPrices.get(0).getSymbol();


    }


    /*
    *
    * 实际下单数量，用最小交易单元来交易
    * */
    public void doTradeTotal(String symbol, BigDecimal totalCost) throws InterruptedException {
        BigDecimal standardTradeUnit = new BigDecimal(BeanConfig.standardTradeUnit);
        while(totalCost.compareTo(standardTradeUnit)>0){
            doTrade(symbol,standardTradeUnit);
            totalCost = totalCost.subtract(standardTradeUnit);
            Thread.sleep(1000);
        }
        doTrade(symbol,totalCost);
    }

    /*
    * 下单，总价为cost
    * @Param symbol 标的
    * @Param cost 下单量的usdt
    *
    * */
    public void doTrade(String symbol, BigDecimal cost) throws InterruptedException {
        logger.info("doTrade start...........");
        BigDecimal bidPrice, askPrice;

        do{
            //re-get the price in the cache
           bidPrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
           askPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
           logger.info("askPrice："+askPrice+" bidprice："+bidPrice);
           Thread.sleep(200);
        } while(bidPrice.subtract(askPrice).abs().divide(askPrice,4).compareTo(new BigDecimal(BeanConfig.priceGap))<0);

        //计算合约最小下单位数
        final String[] stepSize = {"",""};
        MarketCache.futureInfoCache.get(symbol).getFilters().forEach(list->{
            final boolean[] lotSizeFilter = {false};
            list.forEach(stringStringMap -> {
                if(stringStringMap.containsKey("filterType") && stringStringMap.get("filterType").equals("LOT_SIZE")){
                            lotSizeFilter[0] = true;
                }
            });

            if(lotSizeFilter[0]){
                list.forEach(stringStringMap -> {
                    if(stringStringMap.containsKey("stepSize")){
                        stepSize[0] = stringStringMap.get("stepSize");
                    }
                });
            }
        });

        int futureStepSize = stepSize[0].lastIndexOf("1")-stepSize[0].indexOf(".");

        //现货最小下单位数
        for (SymbolFilter symbolFilter : MarketCache.spotInfoCache.get(symbol).getFilters()) {
            if (symbolFilter.getFilterType() == FilterType.LOT_SIZE) {
                stepSize[1] = symbolFilter.getStepSize();
                break;
            }
        }
        int spotStepSize = stepSize[1].lastIndexOf("1")-stepSize[1].indexOf(".");


        //计算合约卖单数量
        BigDecimal futureQuantity =  cost.divide(bidPrice, futureStepSize,BigDecimal.ROUND_HALF_UP);
        //计算现货买单数量
        BigDecimal spotQuantity = cost.divide(bidPrice, spotStepSize,BigDecimal.ROUND_HALF_UP);

        //do the order
        doFutureBid(symbol, bidPrice, futureQuantity, futureStepSize);
        doSpotAsk(symbol, askPrice, spotQuantity, spotStepSize);


    }

    @Async
    public void doFutureBid(String symbol, BigDecimal bidPrice, BigDecimal futureQty, int futureStepSize) throws InterruptedException{
        while (futureQty.compareTo(BigDecimal.ZERO)>0 && bidPrice.multiply(futureQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {
            //下单
            Order order = BinanceClient.futureSyncClient.postOrder(symbol,OrderSide.SELL,null, OrderType.LIMIT, TimeInForce.GTC,futureQty.toString(),
                    bidPrice.toString(),null,null,null,null,NewOrderRespType.RESULT);
            Long orderId = order.getOrderId();
            logger.info("futrue open order: orderid=" + orderId);
            Thread.sleep(BeanConfig.orderExpireTime);
            if(MarketCache.futureOrderCache.containsKey(orderId) &&
                    MarketCache.futureOrderCache.get(orderId).getOrderStatus().equals("FILLED")){
                logger.info("future order has been filled: orderid={}",orderId);
                return;
            }

            try {
                order = BinanceClient.futureSyncClient.cancelOrder(symbol, order.getOrderId(), null);
            }catch (BinanceApiException binanceApiException){
                if (binanceApiException.getMessage().contains("Unknown order sent")) {
                        //order has been filled but subscription not receive, do nothing
                        logger.info("future order has been filled,no need to cancel,orderid={}",orderId);
                    }
            }

            if(order.getExecutedQty() == futureQty){
                return;
            }else{
                Thread.sleep(200);
                BigDecimal orignBidPrice = new BigDecimal(bidPrice.toString());

                bidPrice =  MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                futureQty = orignBidPrice.multiply(order.getOrigQty().subtract(order.getExecutedQty())).divide(bidPrice,
                       futureStepSize);
                logger.info("future's order info,bidPrice={}, futureQty={}",bidPrice,futureQty);
            }
        }
    }

    @Async
    public void doSpotAsk(String symbol, BigDecimal askPrice, BigDecimal spotQty,int spotStepSize) throws InterruptedException{
        while(spotQty.compareTo(BigDecimal.ZERO)>0 && askPrice.multiply(spotQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0){
            NewOrderResponse newOrderResponse = BinanceClient.spotSyncClient.newOrder(limitBuy(symbol, com.binance.api.client.domain.TimeInForce.GTC, spotQty.toString(),
                    askPrice.toString()).newOrderRespType(NewOrderResponseType.FULL));
            Long orderId = newOrderResponse.getOrderId();
            logger.info("new spot order,orderid={}",orderId);
            Thread.sleep(BeanConfig.orderExpireTime);
            if(MarketCache.spotOrderCache.containsKey(orderId) &&
                    MarketCache.spotOrderCache.get(orderId).getOrderStatus() == OrderStatus.FILLED){
                logger.info("spot order has been filled, orderId={}" ,orderId);
                return;
            }
            CancelOrderResponse cancelOrderResponse = null;

            try{
                cancelOrderResponse = BinanceClient.spotSyncClient.cancelOrder(new CancelOrderRequest(symbol, orderId));
            }catch (Exception exception){
                if (exception.getMessage().contains("Unknown order sent")) {
                    //order has been filled but subscription not receive, do nothing
                    logger.info("spot order has been filled,no need to cancel,orderid={}",orderId);
                }
            }


            if(cancelOrderResponse.getExecutedQty() == spotQty.toString()){
                return;
            }else{
                Thread.sleep(200);
                BigDecimal orignAskPrice = new BigDecimal(askPrice.toString());

                askPrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                spotQty = orignAskPrice.multiply(spotQty.subtract(new BigDecimal(cancelOrderResponse.getExecutedQty()))).divide(askPrice,
                        spotStepSize);
                logger.info("spot's order info,bidPrice={}, futureQty={}",askPrice,spotQty);


            }
        }
    }


    public static void main(String[] args) {
       String value = "0.0001";
       System.out.println(new BigDecimal(value));
    }
}
