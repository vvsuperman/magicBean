package com.furiousTidy.magicbean.dbutil.dao;

import com.furiousTidy.magicbean.dbutil.model.OptionModel;
import com.furiousTidy.magicbean.dbutil.model.OrderModel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.Date;

@Mapper
@Component
public interface OptionDao
{
    @Insert("insert into options(symbol, instrumentName,tradeId, price, markPrice, indexPrice,iv,amount,direction,tradeTime,tickDirection) " +
            "values(#{symbol}, #{instrumentName}, #{tradeId}, #{price},#{markPrice},#{indexPrice},#{iv},#{amount},#{direction},#{tradeTime},#{tickDirection})" +
            "on duplicate key update tradeId= #{tradeId}")
    void saveOption(OptionModel optionModel);

    @Select("select count(*) from options where  tradeTime >= #{dateStart} and tradeTime <= #{dateEnd}")
    int getOptionCount(@Param("dateStart") Date dateStart, @Param("dateEnd") Date dateEnd);

    @Select("select count(*) from options where tradeId = #{tradeId}")
    int getOptionById(@Param("tradeId") String tradeId);
}
