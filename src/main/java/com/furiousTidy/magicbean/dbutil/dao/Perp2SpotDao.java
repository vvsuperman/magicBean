package com.furiousTidy.magicbean.dbutil.dao;

import com.furiousTidy.magicbean.dbutil.model.OptionModel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Mapper
@Component
public interface Perp2SpotDao
{
    @Insert("insert into perp2spot(name) values(#{name})")
    void savePerp2Spot(@Param("name") String name);

    @Select("select * from perp2spot")
    List<String> getPerp2Spot();
}
