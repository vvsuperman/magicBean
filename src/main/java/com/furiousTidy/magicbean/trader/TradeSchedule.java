package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.client.model.trade.AccountInformation;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.net.ftp.FtpClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.TreeMap;

import com.binance.api.client.domain.TransferType;

import static com.binance.api.client.domain.account.NewOrder.limitBuy;
import static com.binance.api.client.domain.account.NewOrder.marketBuy;


// schedule move tradeInfo and pairsTrade to his_table
@Service
public class TradeSchedule {

    @Autowired
    public PairsTradeDao pairsTradeDao;

    @Autowired
    public TradeInfoDao tradeInfoDao;

    @Autowired
    TradeUtil tradeUtil;

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;


    //future and spot balance
    public void doFutureSpotBalance(){
        AccountInformation accountInformation =  BinanceClient.futureSyncClient.getAccountInformation();
        final BigDecimal[] balances = new BigDecimal[2];
        accountInformation.getAssets().stream().filter(asset -> asset.getAsset().equals("USDT")).forEach(asset -> {
           balances[0] = asset.getMaxWithdrawAmount();
        });

        BinanceClient.spotSyncClient.getAccount().getBalances().stream().filter(assetBalance -> assetBalance.getAsset().equals("USDT"))
                .forEach(assetBalance -> {
                    balances[1] = new BigDecimal(assetBalance.getFree());
                });

        if(balances[0].subtract(balances[1]).compareTo(BigDecimal.ZERO)>0){
            BinanceClient.marginRestClient.transfer("USDT",balances[0].subtract(balances[1]).divide(new BigDecimal(2),2,RoundingMode.HALF_DOWN).toString(),TransferType.UMFUTURE_MAIN);
        }else if(balances[1].subtract(balances[0]).compareTo(BigDecimal.ZERO)>0){
            BinanceClient.marginRestClient.transfer("USDT",balances[1].subtract(balances[0]).divide(new BigDecimal(2),2,RoundingMode.HALF_DOWN).toString(),TransferType.MAIN_UMFUTURE);
        }
    }

    //buy some bnb for charge
    public void buyBNB(){
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

        //transfer some bnb to u coin
        if(balances[0].multiply(bnbPrice).compareTo(new BigDecimal(10))<0){
            BinanceClient.marginRestClient.transfer("BNB",balances[1].divide(new BigDecimal(2),4,RoundingMode.HALF_DOWN).toString(),TransferType.MAIN_UMFUTURE);
        }

        // buy some bnb
        if(balances[1].multiply(bnbPrice).compareTo(new BigDecimal(10))<0){
            BigDecimal bnbQty = new BigDecimal("15").divide(bnbPrice,stepSize[1],RoundingMode.HALF_UP);
            spotSyncClientProxy.newOrder(
                    marketBuy("BNBUSDT",
                            bnbQty.toString()).newOrderRespType(NewOrderResponseType.FULL));
        }
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
