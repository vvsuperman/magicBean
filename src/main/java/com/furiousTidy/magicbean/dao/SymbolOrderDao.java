package com.furiousTidy.magicbean.dao;

import com.furiousTidy.magicbean.model.SymbolOrderModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.SelectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public class SymbolOrderDao {
    @SelectProvider(type = UserInfoMapper.class,method = "findUserInfoList")
    List<SymbolOrderModel> findSymbolByStatus(String id);
}
