package com.furiousTidy.magicbean.dbutil1;

import com.furiousTidy.magicbean.dbutil1.TradeInfoHis;
import com.furiousTidy.magicbean.dbutil1.TradeInfoHisExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TradeInfoHisMapper {
    long countByExample(TradeInfoHisExample example);

    int deleteByExample(TradeInfoHisExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(TradeInfoHis record);

    int insertSelective(TradeInfoHis record);

    List<TradeInfoHis> selectByExample(TradeInfoHisExample example);

    TradeInfoHis selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") TradeInfoHis record, @Param("example") TradeInfoHisExample example);

    int updateByExample(@Param("record") TradeInfoHis record, @Param("example") TradeInfoHisExample example);

    int updateByPrimaryKeySelective(TradeInfoHis record);

    int updateByPrimaryKey(TradeInfoHis record);
}