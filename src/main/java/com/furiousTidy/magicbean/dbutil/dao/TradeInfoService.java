package com.furiousTidy.magicbean.dbutil.dao;

import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TradeInfoService {

    @Autowired
    TradeInfoDao tradeInfoDao;

    public synchronized Integer insertTradeInfo(TradeInfoModel tradeInfoModel){
        return tradeInfoDao.insertTradeInfo(tradeInfoModel);
    }
}
