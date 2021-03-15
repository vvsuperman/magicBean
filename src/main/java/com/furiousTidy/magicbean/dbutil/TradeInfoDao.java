package com.furiousTidy.magicbean.dbutil;

import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface TradeInfoDao {
    @SelectProvider(type = TradeInfoMapper.class,method = "findSymbolOrderList")
    List<TradeInfoModel> findBySymbolStatusType(@Param("symbolModel") TradeInfoModel symbolModel);

    @InsertProvider(type=TradeInfoMapper.class,method = "insertPairsTrade")
    Integer insertPairsTrade(@Param("pairsTradeModel") TradeInfoModel pairsTradeModel);
}
