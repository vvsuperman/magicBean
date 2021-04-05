package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import com.furiousTidy.magicbean.util.MarketCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.furiousTidy.magicbean.util.MarketCache.futureRateCache;

@Service
public class TradeUtil {

    @Autowired
    TradeInfoDao tradeInfoDao;

    @Autowired
    PairsTradeDao pairsTradeDao;


    public boolean isTradeCanClosed(String symbol){
        if(Boolean.valueOf(BeanConfig.TRADE_ALWAYS_CLOSE)) return true;
        if(futureRateCache.containsKey(symbol)) return false;
        if(futureRateCache.get(symbol).compareTo(new BigDecimal(BeanConfig.FUND_RATE_THRESHOLD))>0){
            return false;
        }
        return true;
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



    //is enough money
    public boolean isUSDTenough(){
        return new BigDecimal(MarketCache.spotBalanceCache.get("USDT").getFree()).compareTo(BeanConfig.STANDARD_TRADE_UNIT)>0
                && MarketCache.futureBalanceCache.get("USDT").getWalletBalance().compareTo(BeanConfig.STANDARD_TRADE_UNIT) >0;
    }

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

        System.out.println((Boolean.valueOf("true")));
      }


}
