package com.furiousTidy.magicbean.trader.service;

import com.binance.client.model.trade.AccountInformation;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.model.SymbolPosition;
import com.furiousTidy.magicbean.util.BinanceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class TradeHelpService {

    @Autowired
    BinanceClient binanceClient;

    public Map getBalanceInfo(){
        AccountInformation accountInformation =  binanceClient.getFutureSyncClient().getAccountInformation();
        final TreeMap<String,BigDecimal> assetInfo = new TreeMap<>();
        accountInformation.getAssets().forEach(asset -> {
            if(!asset.getMaxWithdrawAmount().equals(BigDecimal.ZERO)){
                assetInfo.put(asset.getAsset(),asset.getMaxWithdrawAmount());
            }
        });

        final TreeMap<String, String> positionMap = new TreeMap<>();
        accountInformation.getPositions().stream().filter(position -> new BigDecimal(position.getPositionAmt()).compareTo(BigDecimal.ZERO)!=0).
                forEach(position -> positionMap.put(position.getSymbol(),position.getPositionAmt()));
        HashMap rtmap = new HashMap<>();
//        rtmap.put("balance",availableBalance);
        rtmap.put("futureAsset",assetInfo);
        rtmap.put("futurePosition",positionMap);

        final TreeMap<String, String> spotBalanceMap = new TreeMap<>();
        binanceClient.getSpotSyncClient().getAccount().getBalances().stream().filter(assetBalance -> new BigDecimal(assetBalance.getFree()).compareTo(BigDecimal.ZERO)>0)
                .forEach(assetBalance -> spotBalanceMap.put(assetBalance.getAsset(),assetBalance.getFree()));
        rtmap.put("spotPosition",spotBalanceMap);
        return rtmap;
    }

    @Autowired
    PairsTradeDao pairsTradeDao;

    public Map<String,SymbolPosition> checkPosition(){
           List<SymbolPosition> myPositionList = pairsTradeDao.getSymbolPosition();
           Map postionMap  = getBalanceInfo();
            Map<String,String>  futurePosition = (Map<String, String>) postionMap.get("futurePosition");
            Map<String,String>  spotPosition = (Map<String, String>) postionMap.get("spotPosition");

        Map<String, SymbolPosition> realPositionMap = new HashMap<>();
        futurePosition.forEach((symbol,qty)->{
            SymbolPosition symbolPosition = new SymbolPosition();
            symbolPosition.setSymbol(symbol);
            symbolPosition.setFutureQty(new BigDecimal(qty));
            realPositionMap.put(symbol,symbolPosition);
        });

        spotPosition.forEach((symbol, qty)->{
            String USDTSymbol = symbol+"USDT";
            SymbolPosition symbolPosition = realPositionMap.get(USDTSymbol);
            if(symbolPosition == null){
                symbolPosition = new SymbolPosition();
                symbolPosition.setSymbol(USDTSymbol);
                symbolPosition.setSpotQty(new BigDecimal(qty));
                realPositionMap.put(USDTSymbol, symbolPosition);
            }else{
                symbolPosition.setSpotQty(new BigDecimal(qty));
            }
        });

        Iterator<SymbolPosition> iterator = myPositionList.iterator();
           while (iterator.hasNext()){
               SymbolPosition symbolPosition = iterator.next();
               String symbol = symbolPosition.getSymbol();
               BigDecimal futureQty = symbolPosition.getFutureQty();
               BigDecimal spotQty = symbolPosition.getSpotQty();

               if(realPositionMap.containsKey(symbol)){
                   SymbolPosition realSymbolPosition = realPositionMap.get(symbol);
                   futureQty = futureQty.add(realSymbolPosition.getFutureQty());
                   spotQty = spotQty.subtract(realSymbolPosition.getSpotQty());
                   if(futureQty.compareTo(BigDecimal.ZERO)==0 && spotQty.compareTo(BigDecimal.ZERO)==0) {
                       realPositionMap.remove(symbol);
                   }else{
                       realSymbolPosition.setFutureQty(futureQty);
                       realSymbolPosition.setSpotQty(spotQty);
                   }
               }else{
                   realPositionMap.put(symbol, symbolPosition);
               }
           }
           return realPositionMap;

    }
}
