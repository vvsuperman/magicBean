package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.AllOrdersRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.market.BookTicker;
import com.binance.client.model.market.SymbolOrderBook;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.subscription.FutureSubscription;
import com.furiousTidy.magicbean.subscription.PreTradeService;
import com.furiousTidy.magicbean.subscription.SpotSubscription;
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

    public static boolean watchdog = true;

    @RequestMapping("earnmoney")
    public @ResponseBody void earnMoney() throws InterruptedException {
        doCache();
        Thread.sleep(5000);
        positionOpenService.doPairsTradeRobot();
    }

    @RequestMapping("checksql")
    public @ResponseBody void checkSql(){
        PairsTradeModel pairsTradeModel = new PairsTradeModel();
        pairsTradeModel.setSymbol("BTCUSDT");
        pairsTradeModel.setOpenId("abc123457");
        log.info("insert parisTrade:{}",pairsTradeDao.insertPairsTrade(pairsTradeModel));
        log.info("list:{}",pairsTradeDao.findPairsTradeBySymbol("BTCUSDT"));
//          log.info("list:{}",pairsTradeDao.findPairsTradeOpen());
    }

    @RequestMapping("statuscheck")
    public @ResponseBody void statusCheck(){
    }

    @RequestMapping("switchwatchdog")
    public @ResponseBody void switchWagchDog(){
        watchdog = (watchdog == true)?false:true;
    }

    @RequestMapping("storeallticks")
    public @ResponseBody void storeAllTicks() throws InterruptedException {
        preTradeService.storeTicks();
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
            } while(watchdog);
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
            }while (watchdog);
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
            } while(watchdog);

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

    @RequestMapping("futureallorders/{symbol}")
    public @ResponseBody
    List<com.binance.client.model.trade.Order> getAllOrdersFuture(@PathVariable String symbol){
        return BinanceClient.futureSyncClient.getAllOrders(symbol,null,null,null,null);
    }



    @RequestMapping("docache")
    public @ResponseBody String doCache(){
        //set the position side
//        BinanceClient.futureSyncClient.changePositionSide(true);
        //get symbol info
        preTradeService.futureExchangeInfo();
        preTradeService.spotExchangeInfo();

        //subscribe future ratio
        futureSubscription.futureRatioSubscription();

        //subscribe order update info
        futureSubscription.processFutureCache();
        spotSubscription.processBalanceCache();

        //subscribe bookticker info
        futureSubscription.allBookTickerSubscription();
        spotSubscription.allBookTickSubscription();
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
