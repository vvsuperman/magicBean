package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.client.model.trade.AccountInformation;
import com.furiousTidy.magicbean.apiproxy.ProxyUtil;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.subscription.PreTradeService;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Map;

import com.binance.api.client.domain.TransferType;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;


// schedule service
@Configuration
@EnableScheduling
@Slf4j
public class TradeScheduleService {

    @Autowired
    public PairsTradeDao pairsTradeDao;

    @Autowired
    public TradeInfoDao tradeInfoDao;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    @Autowired
    ProxyUtil proxyUtil;

    @Autowired
    PreTradeService preTradeService;

    //checkNetWork state  test order has no use
//    @Scheduled(cron = "0 0/10 * * * ?")
//    public void checkNetWork() throws InterruptedException {
//        long duration = 0;
//        int n = 5;
//        for(int i=0;i<n;i++){
//            long start = System.currentTimeMillis();
//            BinanceClient.spotSyncClient.newOrderTest(marketBuy("BTCUSDT", "0.001").newOrderRespType(NewOrderResponseType.FULL));
//            duration += System.currentTimeMillis()-start;
//            Thread.sleep(10);
//        }
//
//        if(duration/n > 50){
//            BeanConstant.NETWORK_DELAYED = true;
//            log.info("network delayed, duration={}",duration/n);
//        }else {
//            BeanConstant.NETWORK_DELAYED = false;
//            log.info("network Ok, duration={}",duration/n);
//        }
//
//    }
    @Scheduled(cron = "0 0 1 * * ?")
    public void changeLeverageLevel(){
        preTradeService.changeLeverageLevel(1);
    }

    //change gap according to future rate
    @Scheduled(cron = "0 0/10 * * * ?")
    public void changePairsGap(){
        MarketCache.futureRateCache.entrySet().stream().filter(entry -> entry.getKey().contains("USDT")).forEach(entry ->{
            MarketCache.pairsGapCache.put(entry.getKey()
                    ,entry.getValue().multiply(BigDecimal.valueOf(2)).subtract(BeanConfig.GAP_FACTOR).compareTo(
                            BeanConfig.OPEN_PRICE_GAP)>0
                            ? entry.getValue().multiply(BigDecimal.valueOf(2)).subtract(BeanConfig.GAP_FACTOR)
                            :BeanConfig.OPEN_PRICE_GAP);
                }
        );
    }

    //future and spot balance and synchronize the local balance with the binance exchange
    @Scheduled(cron = "0 0/10 * * * ?")
    public void doFutureSpotBalance() throws InterruptedException {
        BeanConstant.watchdog =false;
        // sleep for 5 second, let on the way order finished, avoid incorrect balance in exchange cache
        Thread.sleep(5000);
        final BigDecimal[] balances = new BigDecimal[2];
        BinanceClient.futureSyncClient.getAccountInformation().getAssets().stream().filter(asset -> asset.getAsset().equals("USDT")).forEach(asset -> {
           balances[0] = asset.getMaxWithdrawAmount();
        });

        BinanceClient.spotSyncClient.getAccount().getBalances().stream().filter(assetBalance -> assetBalance.getAsset().equals("USDT"))
                .forEach(assetBalance -> {
                    balances[1] = new BigDecimal(assetBalance.getFree());
                });

        if(balances[0].subtract(balances[1]).compareTo(BigDecimal.ZERO)>0){
            BigDecimal transferUSDT = balances[0].subtract(balances[1]).divide(new BigDecimal(2),2,RoundingMode.HALF_DOWN);
            BinanceClient.marginRestClient.transfer("USDT",transferUSDT.toString(),TransferType.UMFUTURE_MAIN);
            //synchronize local cache
            while (MarketCache.futureBalance.compareAndSet(MarketCache.futureBalance.get(),balances[0].subtract(transferUSDT)));
            while (MarketCache.spotBalance.compareAndSet(MarketCache.spotBalance.get(),balances[1].add(transferUSDT)));
        }else if(balances[1].subtract(balances[0]).compareTo(BigDecimal.ZERO)>0){
            BigDecimal transferUSDT = balances[1].subtract(balances[0]).divide(new BigDecimal(2),2,RoundingMode.HALF_DOWN);
            BinanceClient.marginRestClient.transfer("USDT",transferUSDT.toString(),TransferType.MAIN_UMFUTURE);
            //synchronize local cache
            while (MarketCache.futureBalance.compareAndSet(MarketCache.futureBalance.get(),balances[0].add(transferUSDT)));
            while (MarketCache.spotBalance.compareAndSet(MarketCache.spotBalance.get(),balances[1].subtract(transferUSDT)));
        }
        BeanConstant.watchdog =true;
    }

    //buy some bnb for exchange charge, check for 1 in the morning
    @Scheduled(cron = "0 0 1 * * ?")
    public void buyBNB(){
        log.info("buy some bnb for exchange charge");
        AccountInformation accountInformation =  BinanceClient.futureSyncClient.getAccountInformation();
        final BigDecimal[] balances = new BigDecimal[2];
        accountInformation.getAssets().stream().filter(asset -> asset.getAsset().equals("BNB")).forEach(asset -> {
            balances[0] = asset.getMaxWithdrawAmount();
        });

        BinanceClient.spotSyncClient.getAccount().getBalances().stream().filter(assetBalance -> assetBalance.getAsset().equals("BNB"))
                .forEach(assetBalance -> {
                    balances[1] = new BigDecimal(assetBalance.getFree());
                });
        BigDecimal bnbPrice = MarketCache.spotTickerMap.get("BNBUSDT").get(BeanConstant.BEST_ASK_PRICE);
        Integer[] stepSize = tradeUtil.getStepSize("BNBUSDT");

        log.info("future bnb={}, spot bnb={}", balances[0],balances[1]);
        //transfer some bnb to u coin
        if(balances[0].multiply(bnbPrice).compareTo(new BigDecimal(10))<0){
            BinanceClient.marginRestClient.transfer("BNB",balances[1].divide(new BigDecimal(2),4,RoundingMode.HALF_DOWN).toString(),TransferType.MAIN_UMFUTURE);
        }

        // buy some bnb
        if(balances[1].multiply(bnbPrice).compareTo(new BigDecimal(10))<0){
            BigDecimal bnbQty = new BigDecimal("15").divide(bnbPrice,stepSize[1],RoundingMode.HALF_UP);
            NewOrderResponse order = spotSyncClientProxy.newOrder(
                    marketBuy("BNBUSDT",
                            bnbQty.toString()).newOrderRespType(NewOrderResponseType.FULL));
        }
    }

    public static void main(String[] args){
        BigDecimal a = new BigDecimal(BigInteger.valueOf(1));
        BigDecimal b = new BigDecimal(BigInteger.valueOf(2));
        System.out.println(b.add(a.negate()));
    }



//    public void subscribeSymbolTick(){
//        BinanceClient.futureSubsptClient.subscribeSymbolBookTickerEvent();
//    }

    /*
    * schedule move tradeInfo and pairsTrade to his_table
    *  if tradeinfo has closeratio and not change between 10s
    * */
    public void bakTradeInfo() throws InterruptedException {
//        List<PairsTradeModel> pairsTradeList = pairsTradeDao.findAllPairsTrade();
//        Thread.sleep(10000);
//        List<PairsTradeModel> pairsTradeList10 = pairsTradeDao.findAllPairsTrade();
//        for(PairsTradeModel pairsTrade: pairsTradeList){
//            for(PairsTradeModel pairsTrade10: pairsTradeList10){
//                if(pairsTrade.getId().equals(pairsTrade10.getId())
//                        && pairsTrade.getCloseRatio()!=null
//                        && pairsTrade.getCloseRatio().equals(pairsTrade10.getCloseRatio())){
//                    pairsTradeHisDao.insertPairsTrade(pairsTrade);
//                    pairsTradeDao.deletePairsTrade(pairsTrade);
//                    TradeInfoModel tradeInfo = tradeInfoDao.getTradeInfoByOpenId(pairsTrade.getOpenId());
//                    tradeInfoHisDao.insertTradeInfo(tradeInfo);
//                    tradeInfoDao.deleteTradeInfo(tradeInfo);
//                }
//            }
//        }

    }

}
