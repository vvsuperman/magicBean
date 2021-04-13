package com.furiousTidy.magicbean.trader.controller;


import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.client.model.enums.NewOrderRespType;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.enums.TimeInForce;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.trader.TradeScheduleService;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.Field;
import java.util.Map;

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

    @RequestMapping("getMoneyEarn/{openIds}")
    public @ResponseBody String getMoneyEarn(@PathVariable String[] openIds){
        for (String openId : openIds) {
            PairsTradeModel pairsTradeModel = pairsTradeDao.getPairsTradeByOpenId(openId);
            TradeInfoModel openTradeInfo = tradeInfoDao.getTradeInfoByOrderId(openId);
            TradeInfoModel closeTradeInfo = tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getCloseId());

        }
        return "success";
    }

    @RequestMapping("futureOrderTest")
    public @ResponseBody String futureOrderTest(){
        Order order = BinanceClient.futureSyncClient.postOrder("BTCUSDT",OrderSide.BUY,null, OrderType.MARKET, null,"0.001",
                null,null,null,null,null, NewOrderRespType.RESULT);
        log.info("future order={}",order);
        log.info("get future order={}",BinanceClient.futureSyncClient.getOrder("BTCUSDT",order.getOrderId(),null));
        return "success";
    }


    @RequestMapping("spotOrderTest")
    public @ResponseBody String spotOrderTest(){
       NewOrderResponse newOrderResponse = spotSyncClientProxy.newOrder(
               marketSell("BTCUSDT","0.003" ).newOrderRespType(NewOrderResponseType.FULL));
       log.info("spot order={}",newOrderResponse);
       log.info("get spot order={}",
         BinanceClient.spotSyncClient.getOrderStatus(new OrderStatusRequest("BTCUSDT", newOrderResponse.getOrderId()))
       );
       return "success";
    }

    @RequestMapping("doBalance")
    public @ResponseBody String doBalance(){
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
