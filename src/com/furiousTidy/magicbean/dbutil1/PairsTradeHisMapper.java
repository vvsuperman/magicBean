package com.furiousTidy.magicbean.dbutil1;

import com.furiousTidy.magicbean.dbutil1.PairsTradeHis;
import com.furiousTidy.magicbean.dbutil1.PairsTradeHisExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PairsTradeHisMapper {
    long countByExample(PairsTradeHisExample example);

    int deleteByExample(PairsTradeHisExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(PairsTradeHis record);

    int insertSelective(PairsTradeHis record);

    List<PairsTradeHis> selectByExample(PairsTradeHisExample example);

    PairsTradeHis selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") PairsTradeHis record, @Param("example") PairsTradeHisExample example);

    int updateByExample(@Param("record") PairsTradeHis record, @Param("example") PairsTradeHisExample example);

    int updateByPrimaryKeySelective(PairsTradeHis record);

    int updateByPrimaryKey(PairsTradeHis record);
}