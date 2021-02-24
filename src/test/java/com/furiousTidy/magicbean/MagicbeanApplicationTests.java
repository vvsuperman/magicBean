package com.furiousTidy.magicbean;


import com.furiousTidy.magicbean.trader.PositionOpen;
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

	@Test
	public void positionTest() throws InterruptedException {
        for(int i=0; i<5; i++){


        }
	}

}
