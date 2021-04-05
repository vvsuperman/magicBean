package com.furiousTidy.magicbean;

import com.furiousTidy.magicbean.trader.TradeSchedule;
import com.furiousTidy.magicbean.trader.controller.PositionOpenController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TradeTests {

    @Autowired
    TradeSchedule tradeSchedule;

    @Autowired
    PositionOpenController positionOpenController;

    @Test
    public void testTransfer(){
        tradeSchedule.doFutureSpotBalance();
//        positionOpenController.doCache();
//        tradeSchedule.buyBNB();
    }
}
