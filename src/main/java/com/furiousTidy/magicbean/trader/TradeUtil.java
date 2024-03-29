package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.MarketCache;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.Mark;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Service
public class TradeUtil {

    public static String getCurrentTime(){
        LocalDate today = LocalDate.now();
        LocalTime time = LocalTime.now();
        return today.getYear()+"/"+today.getMonthValue()+"/"+today.getDayOfMonth()+" "+time;
    }

    //future rate high or not
    public boolean futureSelected(String symbol){
        int i=0;
        for(Map.Entry entry: MarketCache.fRateSymbolCache.entrySet()){
            if( i > BeanConfig.priorNum) {
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
          System.out.println(new BigDecimal(2.00).setScale(0));
      }


}
