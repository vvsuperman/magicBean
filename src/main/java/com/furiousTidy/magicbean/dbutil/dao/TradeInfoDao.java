package com.furiousTidy.magicbean.dbutil.dao;

import com.furiousTidy.magicbean.dbutil.mapper.TradeInfoMapper;
import com.furiousTidy.magicbean.dbutil.model.TradeInfoModel;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Mapper
@Component
public interface TradeInfoDao {
    @Select("Select * from trade_info where orderId = #{orderId}")
    TradeInfoModel getTradeInfoByOrderId(@Param("orderId") String orderId);

    @Insert("INSERT INTO trade_info(orderId,symbol,futurePrice,futureQty,futureTickDelayTime,spotPrice,spotQty,spotTickDelayTime,createTime) " +
            "VALUES(#{orderId}, #{symbol}, #{futurePrice}, #{futureQty},#{futureTickDelayTime},#{spotPrice},#{spotQty},#{spotTickDelayTime},#{createTime})")
    Integer insertTradeInfo(TradeInfoModel tradeInfoModel);

    @Update("UPDATE trade_info SET futurePrice=#{futurePrice},futureQty=#{futureQty}" +
            ",futureTickDelayTime=#{futureTickDelayTime},spotPrice=#{spotPrice},spotQty=#{spotQty},spotTickDelayTime=#{spotTickDelayTime},updateTime=#{updateTime} WHERE orderId =#{orderId}")
    void updateTradeInfoById(TradeInfoModel tradeInfoModel);
}
