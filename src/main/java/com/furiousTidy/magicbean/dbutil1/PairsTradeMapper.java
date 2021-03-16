package com.furiousTidy.magicbean.dbutil1;

import com.furiousTidy.magicbean.dbutil1.PairsTrade;
import com.furiousTidy.magicbean.dbutil1.PairsTradeExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PairsTradeMapper {
    long countByExample(PairsTradeExample example);

    int deleteByExample(PairsTradeExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(PairsTrade record);

    int insertSelective(PairsTrade record);

    List<PairsTrade> selectByExample(PairsTradeExample example);

    PairsTrade selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") PairsTrade record, @Param("example") PairsTradeExample example);

    int updateByExample(@Param("record") PairsTrade record, @Param("example") PairsTradeExample example);

    int updateByPrimaryKeySelective(PairsTrade record);

    int updateByPrimaryKey(PairsTrade record);
}