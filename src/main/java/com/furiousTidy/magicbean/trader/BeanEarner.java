package com.furiousTidy.magicbean.trader;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.CancelOrderResponse;
import com.binance.client.RequestOptions;
import com.binance.client.SubscriptionClient;
import com.binance.client.SyncRequestClient;
import com.binance.client.model.enums.*;
import com.binance.client.model.event.MarkPriceEvent;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.constant.BeanConfig;
import com.furiousTidy.magicbean.constant.BeanConstant;
import com.furiousTidy.magicbean.util.MarketCache;
import com.furiousTidy.magicbean.Subscription.FutureSubscription;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;

public class BeanEarner {
    //合约客户端
    private static SyncRequestClient syncRequestClient = SyncRequestClient.create(BeanConfig.FUTURE_API_KEY, BeanConfig.FUTURE_SECRET_KEY,
            new RequestOptions());

    //现货客户端
    private static BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(BeanConfig.FUTURE_API_KEY, BeanConfig.FUTURE_SECRET_KEY);
    private static BinanceApiRestClient client = factory.newRestClient();

    //监听所有标的
    public void earnMoney(){
        SubscriptionClient client = SubscriptionClient.create(BeanConfig.FUTURE_API_KEY, BeanConfig.FUTURE_SECRET_KEY);

        client.subscribeMarkPricesEvent(((event) -> {
            //将MarkPriceEvent排成有序
            Collections.sort(event, new Comparator<MarkPriceEvent>() {
                public int compare(MarkPriceEvent o1, MarkPriceEvent o2) {
                    return o1.getFundingRate().compareTo(o2.getFundingRate());
                }
             });
            moneyProcess(event);
//           client.unsubscribeAll();
        }), null);
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
    * @Param symbol 标的
    * @Param cost 下单量的usdt
    *
    * */
    public void doTrade(String symbol, BigDecimal cost) throws InterruptedException {

        BigDecimal bidPrice = MarketCache.tickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
        BigDecimal askPrice = MarketCache.tickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);

        //价差不够则不执行
        if(bidPrice.divide(askPrice,4).compareTo(new BigDecimal(BeanConfig.priceGap))<0){
            return;
        }

        //计算合约卖单数量
        BigDecimal futureQuantity =  cost.divide(bidPrice,4);
        //计算现货买单数量
        BigDecimal spotQuantity = cost.divide(askPrice,4);

        doFutureBid(symbol, bidPrice, futureQuantity);
        doSpotAsk(symbol, askPrice, spotQuantity);

    }


    public void doFutureBid(String symbol, BigDecimal bidPrice, BigDecimal futureQty) throws InterruptedException{
        while (futureQty.compareTo(BigDecimal.ZERO)>0) {
            //下单
            Order order = syncRequestClient.postOrder(symbol,OrderSide.SELL,PositionSide.SHORT, OrderType.LIMIT, TimeInForce.GTC,futureQty.toString(),
                    bidPrice.toString(),null,null,null,null,NewOrderRespType.RESULT);
            Thread.sleep(1000);
            order = syncRequestClient.cancelOrder(symbol, order.getOrderId(), null);
            //查询订单
            //order = syncRequestClient.getOrder(symbol,order.getOrderId(),null);
            if(order.getExecutedQty() == futureQty){
                futureQty = BigDecimal.ZERO;
            }else{
                Thread.sleep(1000);
                BigDecimal orignBidPrice = new BigDecimal(bidPrice.toString());

                bidPrice =  MarketCache.tickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                futureQty = orignBidPrice.multiply(order.getOrigQty().subtract(order.getExecutedQty())).divide(bidPrice,4);
            }
        }
    }

    public void doSpotAsk(String symbol, BigDecimal askPrice, BigDecimal spotQty) throws InterruptedException{
        while(spotQty.compareTo(BigDecimal.ZERO)>0){
            NewOrderResponse newOrderResponse = client.newOrder(limitBuy(symbol, com.binance.api.client.domain.TimeInForce.GTC, spotQty.toString(),
                    askPrice.toString()).newOrderRespType(NewOrderResponseType.FULL));
            Thread.sleep(500);
            CancelOrderResponse cancelOrderResponse = client.cancelOrder(new CancelOrderRequest(symbol, newOrderResponse.getOrderId()));
            if(cancelOrderResponse.getExecutedQty() == spotQty.toString()){
                spotQty = BigDecimal.ZERO;
            }else{
                Thread.sleep(500);
                BigDecimal orignAskPrice = new BigDecimal(askPrice.toString());

                askPrice = MarketCache.tickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                spotQty = orignAskPrice.multiply(spotQty.subtract(new BigDecimal(cancelOrderResponse.getExecutedQty()))).divide(askPrice,4);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        FutureSubscription.bookTickerSubscription();
        Thread.sleep(10000);
        BeanEarner beanEarner = new BeanEarner();
        beanEarner.doTrade("BNBUSDT", new BigDecimal(100));

        client.getAllAssets();
    }
}
