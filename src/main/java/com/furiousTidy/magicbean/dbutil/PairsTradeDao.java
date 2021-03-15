package com.furiousTidy.magicbean.dbutil;

import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Component;

import java.util.List;


@Mapper
@Component
public interface PairsTradeDao {
    @SelectProvider(type = PairsTradeMapper.class,method = "findPairsTradeBySymbol")
    List<PairsTradeModel> findPairsTradeBySymbol(@Param("symbol") String symbol);

    @InsertProvider(type=PairsTradeMapper.class,method = "insertPairsStatus")
    Integer insertPairsStatus(@Param("pairsStatusModel") PairsTradeModel pairsStatusModel);

}
