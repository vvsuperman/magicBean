package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.AllOrdersRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.furiousTidy.magicbean.apiproxy.FutureSyncClientProxy;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.subscription.FutureSubscription;
import com.furiousTidy.magicbean.subscription.PreTradeService;
import com.furiousTidy.magicbean.subscription.SpotSubscription;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping(path = "/positionopen")
public class PositionOpenController {

    @Autowired
    PositionOpenService positionOpenService;

    @Autowired
    FutureSubscription futureSubscription;

    @Autowired
    SpotSubscription spotSubscription;

    @Autowired
    PreTradeService preTradeService;


    @RequestMapping("spotorder/{symbol}/{orderId}")
    public @ResponseBody Order getOrderStatusSpot(@PathVariable String symbol, @PathVariable Long orderId){
        OrderStatusRequest orderStatusRequest = new OrderStatusRequest(symbol,orderId);
        return BinanceClient.spotSyncClient.getOrderStatus(orderStatusRequest);
    }

    @RequestMapping("spotallorders/{symbol}")
    public @ResponseBody
    List<Order> getAllOrdersSpot(@PathVariable String symbol){
        AllOrdersRequest allOrdersRequest = new AllOrdersRequest(symbol);
        return BinanceClient.spotSyncClient.getAllOrders(allOrdersRequest);
    }

    @RequestMapping("futureallorders/{symbol}")
    public @ResponseBody
    List<com.binance.client.model.trade.Order> getAllOrdersFuture(@PathVariable String symbol){
        return BinanceClient.futureSyncClient.getAllOrders(symbol,null,null,null,null);
    }



    @RequestMapping("docache")
    public @ResponseBody String doCache(){
        preTradeService.futureExchangeInfo();
        preTradeService.spotExchangeInfo();
        futureSubscription.processFutureCache();
        futureSubscription.allBookTickerSubscription();
        spotSubscription.allBookTickSubscription();
        spotSubscription.processBalanceCache();
        return "success";
    }

    @RequestMapping(value = "testcache/{symbol}", method = RequestMethod.GET)
    public @ResponseBody String testCache(@PathVariable String symbol) {
        String str = MarketCache.spotTickerMap.containsKey(symbol)+":"+ MarketCache.futureTickerMap.containsKey(symbol);
        return str;
    }


    @RequestMapping(value = "doopen/{symbol}/{pty}", method = RequestMethod.GET)
    public void doOpen(@PathVariable String symbol, @PathVariable String pty) throws InterruptedException {
        positionOpenService.doTrade(symbol, new BigDecimal(pty));
    }
}
