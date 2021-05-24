package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.client.model.enums.NewOrderRespType;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.enums.TimeInForce;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.FutureSyncClientProxy;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.binance.api.client.domain.account.NewOrder.limitSell;
import static com.furiousTidy.magicbean.util.MarketCache.futureRateCache;

@Service
@Slf4j
public class TradeUtil {

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;

    @Autowired
    FutureSyncClientProxy futureSyncClientProxy;

    @Autowired
    SpotSyncClientProxy spotSyncClientProxy;

    BinanceClient binanceClient;

    @Autowired
    MarketCache marketCache;

    public void testSubDelay(){
        binanceClient.getSpotSubsptClient().onTickerEvent("BTCUSDT", tickEvent->{
            log.info("sub delay time ={}ms",System.currentTimeMillis() - tickEvent.getEventTime());
        });

    }


    public void closeTrade(List<String> openIds) throws InterruptedException {
        String futurePrice, futureQty, spotPrice, spotQty;
        List<PairsTradeModel> pairsTradeModels = new ArrayList<>() ;
        if(openIds.get(0).equals("all")) {
            pairsTradeModels = pairsTradeDao.getPairsTradeOpen();
        }else{
            for (String openId : openIds) {
                pairsTradeModels.add(pairsTradeDao.getPairsTradeByOpenId(openId));
            }
        }
        closePairsTradeList(pairsTradeModels);

    }

    public void closePairsTradeList(List<PairsTradeModel> pairsTradeModels) throws InterruptedException {
        String futurePrice;
        String futureQty;
        String spotPrice;
        String spotQty;
        for (PairsTradeModel pairsTradeModel : pairsTradeModels) {
            String symbol = pairsTradeModel.getSymbol();
            String clientOrderId = symbol+"_"+BeanConstant.FUTURE_SELL_CLOSE+"_"+ getCurrentTime();
            pairsTradeModel.setCloseId(clientOrderId);
            pairsTradeDao.updatePairsTrade(pairsTradeModel);

            TradeInfoModel tradeInfoModel = tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getOpenId());
//            futurePrice = MarketCache.futureTickerMap.get(tradeInfoModel.getSymbol()).get(BeanConstant.BEST_BID_PRICE).toString();
            futurePrice = MarketCache.futureTickerMap.get(tradeInfoModel.getSymbol()).getBestBidPrice().toString();
            futureQty = tradeInfoModel.getFutureQty().toString();
            log.info("close trade for future ordeid={}, price={}, qty={}", clientOrderId,futurePrice,futureQty);
            Order order;
            try{
                 order =futureSyncClientProxy.postOrder(tradeInfoModel.getSymbol(), OrderSide.BUY,null, OrderType.LIMIT, TimeInForce.GTC
                        ,futureQty,  futurePrice
                        ,null,clientOrderId,null,null, NewOrderRespType.RESULT);
            }catch (Exception ex){
                log.info("force close future exception,need manual operation id={}, exception={}",clientOrderId, ex);
            }

            spotPrice = MarketCache.spotTickerMap.get(tradeInfoModel.getSymbol()).getAskPrice().toString();
            spotQty =  tradeInfoModel.getSpotQty().toString();
            log.info("close trade for spot ordeid={}, price={}, qty={}", clientOrderId,spotPrice,spotQty);
            NewOrderResponse newOrderResponse;
            try {
                newOrderResponse = spotSyncClientProxy.newOrder(
                        limitSell(tradeInfoModel.getSymbol(), com.binance.api.client.domain.TimeInForce.GTC,
                                spotQty, spotPrice)
                                .newOrderRespType(NewOrderResponseType.FULL).newClientOrderId(clientOrderId));
            }catch (Exception ex){
                log.info("force close spot exception,need manual operation id={}, exception={}", clientOrderId, ex);
            }

            marketCache.saveFutureOrder(symbol,clientOrderId);
            marketCache.saveSpotOrder(symbol,clientOrderId);
            Thread.sleep(100);
        }
    }


    public Map caculateProfit(List<String> openIds){

        final Map<String, BigDecimal> rtMap = new HashMap<>();
        openIds.forEach(openId->{

            TradeInfoModel tradeOpenInfo = tradeInfoDao.getTradeInfoByOrderId(openId);
            PairsTradeModel pairsTradeModel = pairsTradeDao.getPairsTradeByOpenId(openId);
            TradeInfoModel tradeCloseInfo = tradeInfoDao.getTradeInfoByOrderId(pairsTradeModel.getCloseId());

            BigDecimal openFuturePrice = tradeOpenInfo.getFuturePrice();
            BigDecimal openSpotPrice = tradeOpenInfo.getSpotPrice();
            BigDecimal closeFuturePrice = tradeCloseInfo.getFuturePrice();
            BigDecimal closeSpotPrice = tradeCloseInfo.getSpotPrice();
            BigDecimal qty = tradeOpenInfo.getFutureQty();


            BigDecimal moneyEarn = getProfit(openFuturePrice, openSpotPrice, closeFuturePrice, closeSpotPrice, qty);
            rtMap.put(openId,moneyEarn);
        });

        return rtMap;
    }

    public BigDecimal getProfit(BigDecimal openFuturePrice, BigDecimal openSpotPrice, BigDecimal closeFuturePrice, BigDecimal closeSpotPrice, BigDecimal qty) {
        BigDecimal spotCommission;
        BigDecimal futureCommission;
        BigDecimal gapPrice;
        spotCommission = openSpotPrice.add(closeSpotPrice).multiply(BigDecimal.valueOf(0.00075));
        futureCommission = openFuturePrice.add(closeFuturePrice).multiply(BigDecimal.valueOf(0.00036));

        if(openFuturePrice.compareTo(closeFuturePrice)>0){
            gapPrice = openFuturePrice.subtract(closeFuturePrice).subtract(
                    openSpotPrice.subtract(closeSpotPrice)
            );

        }else{
            gapPrice = closeSpotPrice.subtract(openSpotPrice).subtract(
                    closeFuturePrice.subtract(openFuturePrice)
            );
        }
        return gapPrice.subtract(spotCommission).subtract(futureCommission).multiply(qty);
    }

    public boolean isTradeCanOpen(String symbol){

        //just stop open new pair trade
        if(Boolean.valueOf(BeanConfig.STOP_TRADE)){
            return false;
        }

        //orginal open ratio has big gap with real open ratio
        if(Boolean.valueOf(BeanConstant.GAP_2_BIG)){
            return false;
        }

//        if(Boolean.valueOf(BeanConfig.TRADE_ALWAYS_OPEN)) {
//            return true;
//        }

        if(futureRateCache.containsKey(symbol) && futureRateCache.get(symbol).compareTo(BigDecimal.ZERO) > 0){
            return true;
        }

        return false;
    }


    public boolean isTradeCanClosed(String symbol){

//        if(Boolean.valueOf(BeanConfig.TRADE_ALWAYS_CLOSE)) {
//            return true;
//        }

        //orginal open ratio has big gap with real open ratio
        if(Boolean.valueOf(BeanConstant.GAP_2_BIG)){
            return false;
        }

        //not in top 10 fundrate list && fundrate < 0.001
        if(!inFutureRatingList(symbol)
//                && futureRateCache.get(symbol).compareTo(new BigDecimal(BeanConfig.FUND_RATE_CLOSE_THRESHOLD)) < 0
                ){
            return true;
        }
        return false;
    }


    public List<String> getSymbolWatchList(){
        List<String> symbolList = new ArrayList<>();
        int i=0;
        for(Map.Entry entry: MarketCache.fRateSymbolCache.entrySet()){
            if( i > BeanConfig.PRIOR_NUM) {
                break;
            }
            symbolList.add(entry.getValue().toString());
            i++;
        }
        pairsTradeDao.getPairsTradeOpen().forEach(pairsTradeModel -> {
            symbolList.add(pairsTradeModel.getSymbol());
        });

        return symbolList;
    }



//    public void checkUSDEnough(){
//        final BigDecimal[] balances = new BigDecimal[2];
//        binanceClient.getFutureSyncClient().getAccountInformation().getAssets().stream().filter(asset -> asset.getAsset().equals("USDT")).forEach(asset -> {
//            balances[0] = asset.getMaxWithdrawAmount();
//        });
//
//        binanceClient.getSpotSyncClient().getAccount().getBalances().stream().filter(assetBalance -> assetBalance.getAsset().equals("USDT"))
//                .forEach(assetBalance -> {
//                    balances[1] = new BigDecimal(assetBalance.getFree());
//                });
//
//
//
//        if(balances[0].subtract(BeanConfig.ENOUTH_MOENY_UNIT).compareTo(BigDecimal.ZERO)>0
//                && balances[1].subtract(BeanConfig.ENOUTH_MOENY_UNIT).compareTo(BigDecimal.ZERO)>0){
//            BeanConstant.ENOUGH_MONEY.set(true);
//        }else{
//            BeanConstant.ENOUGH_MONEY.set(false);
//            log.info("check usdt is not enough: spot={},future={}",balances[0],balances[1]);
//        }
//    }

//    //is enough money
//    public boolean isUSDTenough(){
//        return new BigDecimal(MarketCache.spotBalanceCache.get("USDT").getFree()).compareTo(BeanConfig.ENOUTH_MOENY_UNIT)>0
//                && MarketCache.futureBalanceCache.get("USDT").getWalletBalance().compareTo(BeanConfig.ENOUTH_MOENY_UNIT) >0;
//    }



    public static String getCurrentTime(){
        LocalDate today = LocalDate.now();
        LocalTime time = LocalTime.now();
        return today.getYear()+"_"+today.getMonthValue()+"_"+today.getDayOfMonth()+"_"
                +time.getHour()+"_"+time.getMinute()+"_"+time.getSecond()+"_"+time.get(ChronoField.MILLI_OF_SECOND);
    }


    //future rate high or not
    public boolean inFutureRatingList(String symbol){
        int i=0;
        for(Map.Entry entry: MarketCache.fRateSymbolCache.entrySet()){
            if( i > BeanConfig.PRIOR_NUM) {
                return false;
            }
            if(entry.getValue().equals(symbol)){
                return true;
            }
            i++;
        }
        return false;
    }


    public Integer[] getStepSize(String symbol){
          if(MarketCache.stepSizeCache.containsKey(symbol)){
              return MarketCache.stepSizeCache.get(symbol);
          }
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
          if(futureStepSize <= 1) futureStepSize = 0;

          //现货最小下单位数
          for (SymbolFilter symbolFilter : MarketCache.spotInfoCache.get(symbol).getFilters()) {
              if (symbolFilter.getFilterType() == FilterType.LOT_SIZE) {
                  stepSize[1] = symbolFilter.getStepSize();
                  break;
              }
          }
          int spotStepSize = stepSize[1].lastIndexOf("1")-stepSize[1].indexOf(".");
          if(spotStepSize <= 1) spotStepSize = 0;


          Integer[] rtStepSize ={0,0};
          rtStepSize[0] = futureStepSize;
          rtStepSize[1] = spotStepSize;
          MarketCache.stepSizeCache.put(symbol, rtStepSize);
          return rtStepSize;
      }

      public static void main(String[] args){
          TradeUtil tradeUtil = new TradeUtil();

          System.out.println(tradeUtil.getProfit(
                  new BigDecimal("0.41474"),new BigDecimal("0.41293000")
                  ,new BigDecimal("0.480934"),new BigDecimal("0.48")
                  ,new BigDecimal("36")));

      }


    public BigDecimal getPairsGap(String symbol) {
        return MarketCache.pairsGapCache.containsKey(symbol) ? MarketCache.pairsGapCache.get(symbol): BeanConfig.OPEN_PRICE_GAP;
    }



}
