package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.client.exception.BinanceApiException;
import com.binance.client.model.enums.*;
import com.binance.client.model.event.MarkPriceEvent;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.binance.client.model.market.MarkPrice;
import com.binance.client.model.market.Trade;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.TradeInfoModel;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.limitSell;
import static com.furiousTidy.magicbean.trader.TradeUtil.getCurrentTime;


/*
*  开仓
*
* */
@Service
public class PositionOpenService {
    static Logger logger = LoggerFactory.getLogger(PositionOpenService.class);

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    TradeInfoDao tradeInfoDao;

    //处理资金费率
    public void processFundingRate(){
        MarketCache.markPriceList = BinanceClient.futureSyncClient.getMarkPrice(null);
        Collections.sort( MarketCache.markPriceList, new Comparator<MarkPrice>() {
            @Override
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
    public void doTradeSymbol(String symbol, BigDecimal totalCost, String direct) throws InterruptedException {
        BigDecimal standardTradeUnit = BeanConfig.standardTradeUnit;
        while(totalCost.compareTo(standardTradeUnit) > 0){
            doTrade(symbol,standardTradeUnit,direct);
            totalCost = totalCost.subtract(standardTradeUnit);
            Thread.sleep(1000);
        }
        if(totalCost.compareTo(BigDecimal.ZERO) > 0){
            doTrade(symbol,totalCost,direct);
        }
    }

    /*
    * 下单，总价为cost, divide the total cost
    * @Param symbol 标的
    * @Param cost 下单量的usdt
    *
    * */
    public void doTrade(String symbol, BigDecimal cost, String direct) throws InterruptedException {
        logger.info("doTrade start...........");
        BigDecimal futurePrice, spotPrice = BigDecimal.ZERO;

        if(direct.equals(BeanConstant.FUTURE_SELL)){
            do{
                //re-compare the price in the cache
                futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                logger.info("futurePrice："+ futurePrice +" spotprice："+spotPrice+":"+direct);
                Thread.sleep(200);
                //TODO not support pairs now
            } while(futurePrice.subtract(spotPrice).divide(spotPrice,4)
                    .compareTo(BeanConfig.openPriceGap)<0);
        }else{
            do{
                //re-compare the price in the cache
                futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                logger.info("futurePrice："+ futurePrice +" spotprice："+spotPrice+":"+direct);
                Thread.sleep(200);
                //TODO not support pairs now
            } while(spotPrice.subtract(futurePrice).divide(futurePrice,4)
                    .compareTo(BeanConfig.openPriceGap)<0);
        }

//        doPairsTrade(symbol, cost, futurePrice, spotPrice,direct);
    }

    //the central control to control the pair trade
    private void doPairsTradeRobot() throws InterruptedException {
         BigDecimal futurePrice = null;
         BigDecimal spotPrice = null;
        String symbol = "";
        while(true){

            for(Map.Entry<String,ExchangeInfoEntry> entry: MarketCache.futureInfoCache.entrySet()) {
                    //re-compare the price in the cache
                    symbol = entry.getKey();
                    futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                    spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                    if(futurePrice.subtract(spotPrice).divide(spotPrice,4)
                            .compareTo(BeanConfig.openPriceGap) > 0){

                        String clientOrderId = symbol+":"+BeanConstant.FUTURE_SELL_OPEN+":"+ getCurrentTime();
                        //insert orderId into futureBid_table
                        doPairsTrade(symbol, BeanConfig.standardTradeUnit,futurePrice,spotPrice,
                                BeanConstant.FUTURE_SELL,clientOrderId);
                        //存储symbol和差价，持仓量，以及orderid
                    }else {
                        if(inFutureBid(symbol)){
                            //select futureBid from futureBid where symbol = symbol;
                            List<PairsTradeModel> pairsTradeList =  pairsTradeDao.findPairsTradeBySymbol(symbol);
                            //select the mim one
                            Collections.sort(pairsTradeList,
                                    (Comparator<PairsTradeModel>) (a, b) -> b.getOpenRatio().compareTo(a.getOpenRatio())
                            );
                            PairsTradeModel tempPairsTrade;
                            for(PairsTradeModel pairsTradeModel: pairsTradeList){
                                if(spotPrice.subtract(futurePrice).divide(futurePrice,4,BigDecimal.ROUND_HALF_UP)
                                        .add(pairsTradeModel.getOpenRatio()).compareTo(BeanConfig.closePriceGap) > 0){
                                    //begin to close the symbol
                                    String clientOrderId = symbol+":"+BeanConstant.FUTURE_SELL_CLOSE+":"+ getCurrentTime();
                                    TradeInfoModel tradeInfoModel = tradeInfoDao.findByOpenId(pairsTradeModel.getOpenId());
                                    BigDecimal cost = tradeInfoModel.getFuturePrice().multiply(tradeInfoModel.getFutureQty());
                                    doPairsTrade(symbol, cost,futurePrice,spotPrice,
                                            BeanConstant.FUTURE_SELL_CLOSE,clientOrderId);
                                    pairsTradeModel.setCloseId(clientOrderId);
                                    pairsTradeDao.updataPairsTrade(pairsTradeModel);

                                    break;
                                }

                            }

                        }
                    }
                    Thread.sleep(200);
            }
        }
    }

    //TODO NEED SPEED UP
    public boolean inFutureBid(String symbol){
        //check if the symbol in futureBidRatio
        //select * from futureBid where symbol = symbol
        List<PairsTradeModel> pairsTradeList =  pairsTradeDao.findPairsTradeBySymbol(symbol);
        if (pairsTradeList == null){
            return false;
        }else{
            return true;
        }
    }

    //do paris trade
    private void doPairsTrade(String symbol, BigDecimal cost, BigDecimal futurePrice, BigDecimal spotPrice,
                              String direct,String clientOrderId) throws InterruptedException {
        //计算合约最小下单位数
        Integer[] stepSize = tradeUtil.getStepSize(symbol);
        //计算合约卖单数量
        BigDecimal futureQuantity =  cost.divide(futurePrice, stepSize[0], BigDecimal.ROUND_HALF_UP);
        //计算现货买单数量
        BigDecimal spotQuantity = cost.divide(spotPrice, stepSize[1], BigDecimal.ROUND_HALF_UP);
        logger.info("trade future qty:{} spot qty:{}",futureQuantity,spotQuantity);
        //do the order


        doFutureTrade(symbol, futurePrice, futureQuantity, stepSize[0], direct, clientOrderId);
        doSpotTrade(symbol, spotPrice, spotQuantity, stepSize[1], direct, clientOrderId);
    }


    @Async
    public void doFutureTrade(String symbol, BigDecimal futurePrice, BigDecimal futureQty, int futureStepSize,
                              String direct,String clientOrderId) throws InterruptedException{

        OrderSide orderSide =(direct.equals(BeanConstant.FUTURE_SELL_OPEN))? OrderSide.SELL:OrderSide.BUY;
//        PositionSide positionSide = (direct.equals(BeanConstant.FUTURE_SELL))?PositionSide.SHORT:PositionSide.LONG;
        PositionSide positionSide = null;

        while (futureQty.compareTo(BigDecimal.ZERO)>0 && futurePrice.multiply(futureQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {
            //下单
            Order order = BinanceClient.futureSyncClient.postOrder(symbol,orderSide,positionSide, OrderType.LIMIT, TimeInForce.GTC,futureQty.toString(),
                    futurePrice.toString(),null,clientOrderId,null,null,NewOrderRespType.RESULT);
            Long orderId = order.getOrderId();
            logger.info("futrue new order: orderid=" + orderId);
            Thread.sleep(BeanConfig.orderExpireTime);
            //suscription receive the info
            if(MarketCache.futureOrderCache.containsKey(orderId) &&
                    MarketCache.futureOrderCache.get(orderId).getOrderStatus().equals("FILLED")){
                logger.info("future order has been filled: orderid={}",orderId);
                return;
            }

            try {
                order = BinanceClient.futureSyncClient.cancelOrder(symbol, order.getOrderId(), null);
            }catch (BinanceApiException binanceApiException){
                if (binanceApiException.getMessage().contains("Unknown order sent")) {
                        //order has been filled but no subscription received, do nothing
                        logger.info("future order has been filled,no need to cancel,orderid={}",orderId);
                        return;
                    }
            }

            if(order.getExecutedQty().equals(futureQty)){
                logger.info("future order has been filled until the cancel order: orderid={}",orderId);
                return;
            }else{
                Thread.sleep(200);
                if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                    futurePrice =  MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                    futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                }

                futureQty = futureQty.subtract(order.getExecutedQty().setScale(futureStepSize,RoundingMode.HALF_UP));
                logger.info("future's next order info,bidPrice={}, futureQty={}",futurePrice,futureQty);
            }
        }
    }

    @Async
    public void doSpotTrade(String symbol, BigDecimal spotPrice, BigDecimal spotQty, int spotStepSize,String direct) throws InterruptedException{

        while(spotQty.compareTo(BigDecimal.ZERO)>0 &&
                spotPrice.multiply(spotQty).compareTo(BeanConfig.MIN_OPEN_UNIT)>0) {

            NewOrderResponse newOrderResponse = null;

            if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                 newOrderResponse = spotSyncClientProxy.newOrder(
                        limitBuy(symbol, com.binance.api.client.domain.TimeInForce.GTC,
                                spotQty.toString(),
                                spotPrice.toString()).newOrderRespType(NewOrderResponseType.FULL));
            }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                newOrderResponse = spotSyncClientProxy.newOrder(
                        limitSell(symbol, com.binance.api.client.domain.TimeInForce.GTC,
                                spotQty.toString(),
                                spotPrice.toString()).newOrderRespType(NewOrderResponseType.FULL));
            }


            Long orderId = newOrderResponse.getOrderId();
            logger.info("new spot order,orderid={}", orderId);
            Thread.sleep(BeanConfig.orderExpireTime);
            if (MarketCache.spotOrderCache.containsKey(orderId) &&
                    MarketCache.spotOrderCache.get(orderId).getOrderStatus() == OrderStatus.FILLED) {
                logger.info("spot order has been filled, orderId={}", orderId);
                return;
            }
            CancelOrderResponse cancelOrderResponse = null;
            //cancel the order
            try {
                cancelOrderResponse = BinanceClient.spotSyncClient.cancelOrder(new CancelOrderRequest(symbol, orderId));
            } catch (Exception exception) {
                if (exception.getMessage().contains("Unknown order sent")) {
                    //order has been filled but subscription not receive, do nothing
                    logger.info("spot order has been filled,no need to cancel,orderid={}", orderId);
                    return;
                }
            }

            //new order again
            if (cancelOrderResponse.getExecutedQty() == spotQty.toString()) {
                return;
            } else {
                Thread.sleep(200);
                if(direct.equals(BeanConstant.FUTURE_SELL_OPEN)){
                    spotPrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                }else if(direct.equals(BeanConstant.FUTURE_SELL_CLOSE)){
                    spotPrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                }
                spotQty = spotQty.subtract(new BigDecimal(cancelOrderResponse.getExecutedQty()).setScale(spotStepSize, RoundingMode.HALF_UP));
                logger.info("spot's order info,spotPrice={}, spotQty={}", spotPrice, spotQty);
            }
        }
    }




    public static void main(String[] args) {
//       String value = "0.0001";
//        MathContext mathContext = new MathContext(2,RoundingMode.HALF_UP);
//        System.out.println(new BigDecimal(value));
        LocalDate today = LocalDate.now();

        LocalTime time = LocalTime.now();
        System.out.println(today.getYear()+"/"+today.getMonthValue()+"/"+today.getDayOfMonth()+" "+time);

    }
}
