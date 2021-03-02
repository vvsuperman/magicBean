package com.furiousTidy.magicbean.trader;

import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.MarketCache;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.Mark;

import java.math.BigDecimal;

@Service
public class TradeUtil {

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

          //现货最小下单位数
          for (SymbolFilter symbolFilter : MarketCache.spotInfoCache.get(symbol).getFilters()) {
              if (symbolFilter.getFilterType() == FilterType.LOT_SIZE) {
                  stepSize[1] = symbolFilter.getStepSize();
                  break;
              }
          }
          int spotStepSize = stepSize[1].lastIndexOf("1")-stepSize[1].indexOf(".");
          Integer[] rtStepSize ={0,0};
          rtStepSize[0] = futureStepSize;
          rtStepSize[1] = spotStepSize;
          MarketCache.stepSizeCache.put(symbol, rtStepSize);
          return rtStepSize;
      }


}
