package com.furiousTidy.magicbean.dbutil1;

import com.furiousTidy.magicbean.dbutil1.TradeInfo;
import com.furiousTidy.magicbean.dbutil1.TradeInfoExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TradeInfoMapper {
    long countByExample(TradeInfoExample example);

    int deleteByExample(TradeInfoExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(TradeInfo record);

    int insertSelective(TradeInfo record);

    List<TradeInfo> selectByExample(TradeInfoExample example);

    TradeInfo selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") TradeInfo record, @Param("example") TradeInfoExample example);

    int updateByExample(@Param("record") TradeInfo record, @Param("example") TradeInfoExample example);

    int updateByPrimaryKeySelective(TradeInfo record);

    int updateByPrimaryKey(TradeInfo record);
}