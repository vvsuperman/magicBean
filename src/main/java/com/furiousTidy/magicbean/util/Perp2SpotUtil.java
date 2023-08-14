package com.furiousTidy.magicbean.util;

import com.furiousTidy.magicbean.dbutil.dao.Perp2SpotDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Perp2SpotUtil {

    @Autowired
    MarketCache marketCache;

    @Autowired
    Perp2SpotDao perp2SpotDao;

    public void loadPerpCacheFromDB(){
        MarketCache.perp2SpotList = perp2SpotDao.getPerp2Spot();
    }

    public boolean containPerpName(String name){
        if(MarketCache.perp2SpotList.contains(name))
            return true;
        List perpNames = perp2SpotDao.getPerp2Spot();
        if(perpNames.contains(name)){
            MarketCache.perp2SpotList.add(name);
            return true;
        }
        return false;
    }

    public void savePerpName(String name){
        MarketCache.perp2SpotList.add(name);
        perp2SpotDao.savePerp2Spot(name);

    }


}
