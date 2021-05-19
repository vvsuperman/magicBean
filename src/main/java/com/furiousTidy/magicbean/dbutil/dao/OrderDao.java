package com.furiousTidy.magicbean.dbutil.dao;


import com.furiousTidy.magicbean.dbutil.model.OrderModel;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface OrderDao {
    @Select("select * from trade_order" )
    List<OrderModel> getOrders();


    @Insert("insert into trade_order(symbol, clientOrderId, type) values(#{symbol}, #{clientOrderId},#{type})")
    void saveOrder(OrderModel orderModel);

    @Delete("delete from trade_order where clientOrderId = #{clientOrderId} and type=#{type}")
    void deleteOrder(@Param("clientOrderId") String clientOrderId, @Param("type") String type);
}
