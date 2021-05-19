package com.furiousTidy.magicbean.dbutil.dao;

import com.furiousTidy.magicbean.dbutil.mapper.PairsTradeMapper;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


@Mapper
@Component
public interface PairsTradeDao {
    @Select("Select * from pairs_trade where symbol = #{symbol}")
    List<PairsTradeModel> getPairsTradeBySymbol(String symbol);

    @Insert("INSERT INTO pairs_trade(symbol,openId,closeId,openRatio,closeRatio,origOpenRatio,origCloseRatio,createTime) " +
            "VALUES(#{symbol}, #{openId}, #{closeId},#{openRatio},#{closeRatio},#{origOpenRatio},#{origCloseRatio},#{createTime})")
    Integer insertPairsTrade( PairsTradeModel PairsTradeModel);

    @Update("UPDATE pairs_trade SET openId=#{openId},openRatio=#{openRatio}" +
            ",closeId=#{closeId}, closeRatio=#{closeRatio},profit=#{profit},origOpenRatio=#{origOpenRatio},origCloseRatio=#{origCloseRatio},updateTime=#{updateTime} WHERE id =#{id}")
    void updatePairsTrade(PairsTradeModel pairsTradeModel);

    @Update("UPDATE pairs_trade SET openRatio=#{openRatio} WHERE openId =#{openId}")
    void updateOpenRatioByOpenId(@Param("openId") String openId, @Param("openRatio") BigDecimal openRatio);

    @Select("select * from pairs_trade where closeRatio is null")
    List<PairsTradeModel> getPairsTradeOpen();

    @Select("select * from pairs_trade where closeId is null and createTime < #{createTime}")
    List<PairsTradeModel> getPairsTradeOpenByDate(@Param("createTime") String createTime);

    @Select("select * from pairs_trade where openId = #{openId}")
    PairsTradeModel getPairsTradeByOpenId(String openId);

    @Select("select * from pairs_trade where closeId = #{closeId}")
    PairsTradeModel getPairsTradeByCloseId(String closeId);

    @Update("update  pairs_trade set closeId=null where id=#{id}")
    void setCloseId2Null(String id);
}
