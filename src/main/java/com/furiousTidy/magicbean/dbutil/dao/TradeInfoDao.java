package com.furiousTidy.magicbean.dbutil.dao;

import com.furiousTidy.magicbean.dbutil.mapper.TradeInfoMapper;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface TradeInfoDao {
    @Select("Select * from trade_info where orderId = #{orderId}")
    TradeInfoModel getTradeInfoByOrderId(@Param("orderId") String  orderId);

    @InsertProvider(type=TradeInfoMapper.class,method = "insertTradeInfo")
    Integer insertTradeInfo(@Param("tradeInfoModel") TradeInfoModel tradeInfoModel);

    @Update("UPDATE trade_info SET futurePrice=#{futurePrice},futureQty=#{futureQty}" +
            ",spotPrice=#{spotPrice},spotQty=#{spotQty} WHERE orderId =#{orderId}")
    void updateTradeInfoById(TradeInfoModel tradeInfoModel);
}
