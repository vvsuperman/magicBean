package com.furiousTidy.magicbean.apiproxy;

import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.util.MarketCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class ProxyUtil {

    public void adjustBalance(String orderId, BigDecimal price, BigDecimal qty){
        BigDecimal adjustCost = BeanConfig.STANDARD_TRADE_UNIT.subtract(price).multiply(qty);
        boolean success= false;
        while (!success){
            log.info("adjust balance, clientId={},origin={}, gap={}",orderId, MarketCache.spotBalance.get(),adjustCost);
            success = MarketCache.spotBalance.compareAndSet( MarketCache.spotBalance.get(), MarketCache.spotBalance.get().add(adjustCost));
        }
    }


    public void changeBalance(BigDecimal num, String type){
        if(type.equals("spot")){
            while (!MarketCache.spotBalance.compareAndSet( MarketCache.spotBalance.get()
                    , MarketCache.spotBalance.get().add(num)
            )){
                log.info("change spot balance:balance={},num={}",MarketCache.spotBalance.get(), num);
            }
        }else{
            while (!MarketCache.futureBalance.compareAndSet( MarketCache.futureBalance.get()
                    , MarketCache.futureBalance.get().add(num)
            )){
                log.info("change future balance: balance={},num={}",MarketCache.futureBalance.get(), num);

            }
        }

    }


}
