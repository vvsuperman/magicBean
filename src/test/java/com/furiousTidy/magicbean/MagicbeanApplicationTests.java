package com.furiousTidy.magicbean;


import com.furiousTidy.magicbean.Subscription.FutureSubscription;
import com.furiousTidy.magicbean.Subscription.SpotSubscription;
import com.furiousTidy.magicbean.trader.PositionOpenService;
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
    PositionOpenService positionOpenService;

    @Autowired
    FutureSubscription futureSubscription;

    @Autowired
    SpotSubscription spotSubscription;

	@Test
	public void positionTest() throws InterruptedException {
	    futureSubscription.allBookTickerSubscription();
//        spotSubscription.allBookTickSubscription();
        String symbol = "bnbusdt";
        spotSubscription.symbolBookTickSubscription(symbol);

        Thread.sleep(3000);
        positionOpenService.doTrade(symbol,BigDecimal.valueOf(100));
        while(true){}
	}

}
