package com.furiousTidy.magicbean;


import com.furiousTidy.magicbean.Subscription.FutureSubscription;
import com.furiousTidy.magicbean.Subscription.SpotSubscription;
import com.furiousTidy.magicbean.trader.PositionOpen;
import com.furiousTidy.magicbean.util.MarketCache;
import javafx.scene.effect.Light;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

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
	    futureSubscription.bookTickerSubscription();
        spotSubscription.allBookTickSubscription();
        String symbol = "BCHUSDT";
        Thread.sleep(3000);
        positionOpen.doTrade(symbol,BigDecimal.valueOf(100));
        while(true){}
	}

}
