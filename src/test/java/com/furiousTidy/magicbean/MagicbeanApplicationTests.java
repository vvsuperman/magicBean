package com.furiousTidy.magicbean;


import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.SymbolInfo;
import com.furiousTidy.magicbean.Subscription.FutureSubscription;
import com.furiousTidy.magicbean.Subscription.SpotSubscription;
import com.furiousTidy.magicbean.trader.PositionOpen;
import com.furiousTidy.magicbean.util.BinanceClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MagicbeanApplicationTests {

    @Autowired
    PositionOpen positionOpen;

    @Autowired
    FutureSubscription futureSubscription;

    @Autowired
    SpotSubscription spotSubscription;

	@Test
	public void positionTest() throws InterruptedException {
        ExchangeInfo exchangeInfo = BinanceClient.spotSyncClient.getExchangeInfo();
        String symbol = "btcusdt";
//        spotSubscription.onDepthEvent(symbol);
        spotSubscription.symbolBookTickSubscription(symbol);
//        spotSubscription.allBookTickSubscription();


//        for(SymbolInfo symbolInfo:exchangeInfo.getSymbols()){
//            String symbol = symbolInfo.getSymbol();
//            System.out.println(symbol);
//            spotSubscription.onDepthEvent(symbol);
////            futureSubscription.symbolBookTickerSubscription(symbol);
//        }

//	    String symbol = "BTCUSDT";
//	    futureSubscription.allBookTickerSubscription();
//	    futureSubscription.symbolBookTickerSubscription(symbol);
//        spotSubscription.symbolBookTickSubscription(symbol);
//        Thread.sleep(3000);
//        while(true){
//            if(MarketCache.spotTickerMap.containsKey(symbol) && MarketCache.futureTickerMap.containsKey(symbol)){
//                break;
//            }
//        }
//        positionOpen.doTrade(symbol,BigDecimal.valueOf(100));
        while(true){}
	}

}
