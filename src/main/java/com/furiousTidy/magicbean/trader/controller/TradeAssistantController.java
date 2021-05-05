package com.furiousTidy.magicbean.trader.controller;


import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.client.model.enums.NewOrderRespType;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.enums.TimeInForce;
import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.FutureSyncClientProxy;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.trader.TradeScheduleService;
import com.furiousTidy.magicbean.trader.TradeUtil;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

@Controller
@RequestMapping(path = "/tradeassistant")
@Slf4j
public class TradeAssistantController {

    @Autowired
    TradeScheduleService tradeSchedule;

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    FutureSyncClientProxy futureSyncClientProxy;
    
    @Autowired
    BinanceClient binanceClient;

    @RequestMapping("closedTrade3DaysAgo")
    public @ResponseBody String closedTrade3DaysAgo(){
        tradeSchedule.closedTrade3DaysAgo();
        return "sucess";
    }

    @RequestMapping("queryOrder")
    public @ResponseBody String queryOrder(){
        tradeSchedule.queryOrder();
        return "success";
    }

    @RequestMapping("closeTrade/{openIds}")
    public @ResponseBody String closeTrade(@PathVariable List<String> openIds){
         tradeUtil.closeTrade(openIds);
         return "success";
    }

    @RequestMapping("closeAllPosition")
    public @ResponseBody String closeAllPosition(){
//        AccountInformation accountInformation =  binanceClient.getFutureSyncClient().getAccountInformation();
//        final TreeMap<String,BigDecimal> assetInfo = new TreeMap<>();
//        accountInformation.getAssets().forEach(asset -> {
//            if(!asset.getMaxWithdrawAmount().equals(BigDecimal.ZERO)){
//                String clientOrderId = asset.getAsset()+"_"+BeanConstant.FUTURE_SELL_CLOSE+"_"+ tradeUtil.getCurrentTime();
//
//                Order order =futureSyncClientProxy.postOrder(asset.getAsset(), OrderSide.BUY,null, OrderType.LIMIT, TimeInForce.GTC
//                        ,futureQty,  futurePrice
//                        ,null,clientOrderId,null,null, NewOrderRespType.RESULT);
//
//                assetInfo.put(asset.getAsset(),asset.getMaxWithdrawAmount());
//            }
//        });
//
//        final TreeMap<String, String> positionMap = new TreeMap<>();
//        accountInformation.getPositions().stream().filter(position -> new BigDecimal(position.getPositionAmt()).compareTo(BigDecimal.ZERO)!=0).
//                forEach(position -> positionMap.put(position.getSymbol(),position.getPositionAmt()));
//        HashMap rtmap = new HashMap<>();
////        rtmap.put("balance",availableBalance);
//        rtmap.put("asset",assetInfo);
//        rtmap.put("position",positionMap);
        return "success";
    }

    @RequestMapping("getMoneyEarn/{openIds}")
    public @ResponseBody Map getMoneyEarn(@PathVariable List<String> openIds){
        return tradeUtil.caculateProfit(openIds);
    }

    @RequestMapping("futureOrderTest")
    public @ResponseBody String futureOrderTest(){
        Order order = binanceClient.getFutureSyncClient().postOrder("BTCUSDT",OrderSide.BUY,null, OrderType.MARKET, null,"0.001",
                null,null,null,null,null, NewOrderRespType.RESULT);
        log.info("future order={}",order);
        log.info("get future order={}",binanceClient.getFutureSyncClient().getOrder("BTCUSDT",order.getOrderId(),null));
        return "success";
    }


    @RequestMapping("spotOrderTest")
    public @ResponseBody String spotOrderTest(){
       NewOrderResponse newOrderResponse = spotSyncClientProxy.newOrder(
               marketSell("BTCUSDT","0.003" ).newOrderRespType(NewOrderResponseType.FULL));
       log.info("spot order={}",newOrderResponse);
       log.info("get spot order={}",
         binanceClient.getSpotSyncClient().getOrderStatus(new OrderStatusRequest("BTCUSDT", newOrderResponse.getOrderId()))
       );
       return "success";
    }

    @RequestMapping("doBalance")
    public @ResponseBody String doBalance() throws InterruptedException {
        tradeSchedule.doFutureSpotBalance();
        return "success";
    }

    @RequestMapping("buyBNB")
    public @ResponseBody String buyBNB(){
        tradeSchedule.buyBNB();
        return "success";
    }

    @RequestMapping("getFundRateList")
    public @ResponseBody Map getFundRateList(){
        return MarketCache.fRateSymbolCache;
    }


    @RequestMapping("getCache/{name}")
    public @ResponseBody Object getCacheByName(@PathVariable String name) throws Exception {
        Object o = MarketCache.class.newInstance();
        Field f = MarketCache.class.getField(name);
        return f.get(o);
    }
}
