package com.furiousTidy.magicbean.trader.controller;

import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.AllOrdersRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.market.BookTicker;
import com.binance.client.model.market.SymbolOrderBook;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.subscription.FutureSubscription;
import com.furiousTidy.magicbean.subscription.PreTradeService;
import com.furiousTidy.magicbean.subscription.SpotSubscription;
import com.furiousTidy.magicbean.trader.TradeScheduleService;
import com.furiousTidy.magicbean.trader.service.PositionOpenService;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping(path = "/positionopen")
@Slf4j
public class PositionOpenController {

    @Autowired
    PositionOpenService positionOpenService;

    @Autowired
    FutureSubscription futureSubscription;

    @Autowired
    SpotSubscription spotSubscription;

    @Autowired
    PreTradeService preTradeService;

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    TradeScheduleService tradeScheduleService;

    static boolean robotStart = false;


    @RequestMapping("changeLeverageLevel/{level}")
    public @ResponseBody String changeLeverageLevel(@PathVariable int level){
        preTradeService.changeLeverageLevel(level);
        return "success";
    }



    @RequestMapping("getCurrentPrice/{symbol}")
    public @ResponseBody Map getCurrentTick(@PathVariable String symbol){
        Map<String, Object> tickMap = new HashMap();

        if(symbol == null ){
            List<SymbolOrderBook> futureOrderBookList =  BinanceClient.futureSyncClient.getSymbolOrderBookTicker(null);
            List<BookTicker> bookTickerList = spotSyncClientProxy.getAllBookTickers();
            tickMap.put("futureQuery",futureOrderBookList);
            tickMap.put("futurecache",MarketCache.futureTickerMap);
            tickMap.put("spotQuery",bookTickerList);
            tickMap.put("spotcache",MarketCache.spotTickerMap);
        }else{
            List<SymbolOrderBook> futureOrderBookList =  BinanceClient.futureSyncClient.getSymbolOrderBookTicker(symbol);
            BookTicker bookTicker = spotSyncClientProxy.getBookTicker(symbol);
            tickMap.put("futureQuery",futureOrderBookList);
            tickMap.put("futurecache",MarketCache.futureTickerMap.get(symbol));
            tickMap.put("spotQuery",bookTicker);
            tickMap.put("spotcache",MarketCache.spotTickerMap.get(symbol));
        }
        tickMap.put("currentTime", LocalTime.now().getHour()+":"+LocalTime.now().getMinute()+":"+LocalTime.now().getSecond());
        return tickMap;
    }

    @RequestMapping("getHighFutureRateList")
    public @ResponseBody List<String> getHighFutureRateList() {
        int i = 0;
        List<String> symbolList = new ArrayList<>();
        for (Map.Entry<BigDecimal, String> entry : MarketCache.fRateSymbolCache.entrySet()) {
            if (i > BeanConfig.PRIOR_NUM) {
                return symbolList;
            }
            symbolList.add(entry.getValue());
            i++;
        }
        return symbolList;
    }


    @RequestMapping("statuscheck")
    public @ResponseBody String checkStatus(){
        return "magicbean start success";
    }

    @RequestMapping("earnmoney")
    public @ResponseBody String earnMoney() throws InterruptedException {
        log.info("earn money begin..............");
        if(robotStart) {
            return "earn money already start!";
        }
        BeanConstant.watchdog = true;
        robotStart = true;
        positionOpenService.doPairsTradeRobot();
        return "earn money start success";
    }

    @RequestMapping("checksql")
    public @ResponseBody void checkSql(){
        String clientOrderId = "LTCUSDT_FSO_2021_3_18_23_33_44";
        BigDecimal ratio = new BigDecimal("0.006");
        PairsTradeModel pairsTradeModel = new PairsTradeModel();
        pairsTradeModel.setOpenId(clientOrderId);
        pairsTradeModel.setOpenRatio(ratio);
//        pairsTradeDao.insertPairsTrade(pairsTradeModel);
        log.info("pairsTrade={}",pairsTradeDao.getPairsTradeByOpenId("LTCUSDT_FSO_2021_3_18_23_33_44"));
    }


    @RequestMapping("switchwatchdog")
    public @ResponseBody boolean switchWagchDog(){
        BeanConstant.watchdog = (BeanConstant.watchdog == true)?false:true;
        return BeanConstant.watchdog;
    }

    @RequestMapping("storeallticks")
    public @ResponseBody String storeAllTicks() throws InterruptedException {
        log.info("store all ticks.............");
        preTradeService.storeTicks();
        return "store all ticks success";
    }

    @RequestMapping("futurespotratio/{listSymbol}")
    public @ResponseBody void futureSpotRatio(@PathVariable String listSymbol) throws InterruptedException {
        if(!listSymbol.equals("all")){
            do{
                String[] symbols = listSymbol.split("-");
                List<BigDecimal> ratioList = new LinkedList<>();
                //re-compare the price in the cache
                for(String symbol : symbols){
                    BigDecimal futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                    BigDecimal spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                    ratioList.add(futurePrice.subtract(spotPrice).divide(spotPrice,4));
                    Thread.sleep(200);
                }
                log.info("ratio:{}",ratioList);
            } while(BeanConstant.watchdog);
        }
        else if(listSymbol.equals("all")){
            TreeMap<BigDecimal,String > ratioMap = new TreeMap<>(
                    (o1, o2) -> o2.compareTo(o1));
            do{
                ratioMap.clear();
                for(Map.Entry<String, HashMap<String, BigDecimal>> entrySet:MarketCache.futureTickerMap.entrySet()){
                    BigDecimal futurePrice = entrySet.getValue().get(BeanConstant.BEST_BID_PRICE);
                    if(!MarketCache.spotTickerMap.containsKey(entrySet.getKey()))  continue;
                    BigDecimal spotPrice = MarketCache.spotTickerMap.get(entrySet.getKey()).get(BeanConstant.BEST_ASK_PRICE);
                    BigDecimal ratio = futurePrice.subtract(spotPrice).divide(spotPrice,4);
                    String symbol = entrySet.getKey();
                    BigDecimal fundingRate = MarketCache.futureRateCache.get(symbol);
                    BigDecimal totalRatio = ratio.add(fundingRate);
                    String value = symbol+":"+ratio+":"+fundingRate;
                    ratioMap.put(totalRatio,value);
                }
                log.info("ratio:{}",ratioMap);
                Thread.sleep(1000);
            }while (BeanConstant.watchdog);
        }
    }

    @RequestMapping("spotfutureratio/{listSymbol}")
    public @ResponseBody void spotfutureratio(@PathVariable String listSymbol) throws InterruptedException {

            do{
                String[] symbols = listSymbol.split("-");
                List<BigDecimal> ratioList = new LinkedList<>();
                //re-compare the price in the cache
                for(String symbol : symbols){
                    BigDecimal futurePrice = MarketCache.futureTickerMap.get(symbol).get(BeanConstant.BEST_ASK_PRICE);
                    BigDecimal spotPrice = MarketCache.spotTickerMap.get(symbol).get(BeanConstant.BEST_BID_PRICE);
                    ratioList.add(spotPrice.subtract(futurePrice).divide(futurePrice,4));
                    Thread.sleep(200);
                }
                log.info("ratio:{}",ratioList);
            } while(BeanConstant.watchdog);

    }

    @RequestMapping("spotbookticker/{symbol}")
    public @ResponseBody BookTicker getSpotBookTicker(@PathVariable String symbol){
        return BinanceClient.spotSyncClient.getBookTicker(symbol);
    }

    @RequestMapping("futurebookticker/{symbol}")
    public @ResponseBody List<SymbolOrderBook>  getFutureBookTicker(@PathVariable String symbol){
        return BinanceClient.futureSyncClient.getSymbolOrderBookTicker(symbol);
    }


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

    @RequestMapping("futureorders/{symbol}")
    public @ResponseBody List<com.binance.client.model.trade.Order> getAllOrdersFuture(@PathVariable String symbol){
        return BinanceClient.futureSyncClient.getAllOrders(symbol,null,null,null,null);
    }

    @RequestMapping("futureallorders")
    public @ResponseBody List<com.binance.client.model.trade.Order> getAllOrdersFuture(){
        return BinanceClient.futureSyncClient.getAllOrders(null,null,null,null,null);
    }



    @RequestMapping("docache")
    public @ResponseBody String doCache(){
        //set the position side
//        BinanceClient.futureSyncClient.changePositionSide(true);
        //get symbol info
        preTradeService.futureExchangeInfo();
        preTradeService.spotExchangeInfo();

        //subscribe future ratio
        futureSubscription.fundingRateSub();

        //subscribe order update info
//        futureSubscription.processFutureCache();
//        spotSubscription.processBalanceCache();

        //subscribe bookticker info
        futureSubscription.allBookTickerSubscription();
        spotSubscription.allBookTickSubscription();

        //get pairs trade gap
        tradeScheduleService.changePairsGap();

        //set balance
        preTradeService.initialBalance();
        return "success";
    }

    @RequestMapping(value = "testcache/{symbol}", method = RequestMethod.GET)
    public @ResponseBody String testCache(@PathVariable String symbol) {
        String str = MarketCache.spotTickerMap.containsKey(symbol)+":"+ MarketCache.futureTickerMap.containsKey(symbol);
        return str;
    }


    @RequestMapping(value = "doopen/{symbol}/{pty}/{direct}", method = RequestMethod.GET)
    public void doOpen(@PathVariable String symbol, @PathVariable String pty, @PathVariable String direct) throws InterruptedException {
        positionOpenService.doTrade(symbol, new BigDecimal(pty) ,direct);
    }
}
