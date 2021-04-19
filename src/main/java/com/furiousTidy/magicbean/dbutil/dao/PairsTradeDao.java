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

    @Insert("INSERT INTO pairs_trade(symbol,openId,closeId,openRatio,closeRatio) " +
            "VALUES(#{symbol}, #{openId}, #{closeId},#{openRatio},#{closeRatio})")
    Integer insertPairsTrade( PairsTradeModel PairsTradeModel);

    @Update("UPDATE pairs_trade SET openId=#{openId},openRatio=#{openRatio}" +
            ",closeId=#{closeId}, closeRatio=#{closeRatio},profit=#{profit} WHERE id =#{id}")
    void updatePairsTrade(PairsTradeModel pairsTradeModel);

    @Update("UPDATE pairs_trade SET openRatio=#{openRatio} WHERE openId =#{openId}")
    void updateOpenRatioByOpenId(@Param("openId") String openId, @Param("openRatio") BigDecimal openRatio);

    @Update("UPDATE pairs_trade SET closeRatio=#{closeRatio} WHERE closeId =#{closeId}")
    void updateCloseRatioByCloseId( @Param("closeId") String closeId, @Param("closeRatio") BigDecimal closeRatio);

    @Select("select * from pairs_trade where closeId is null")
    List<PairsTradeModel> getPairsTradeOpen();

    @Select("select * from pairs_trade where openId = #{openId}")
    PairsTradeModel getPairsTradeByOpenId(String openId);

    @Select("select * from pairs_trade where closeId = #{closeId}")
    PairsTradeModel getPairsTradeByCloseId(String closeId);
}
