package com.furiousTidy.magicbean.trader.service;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TransferType;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.client.model.enums.NewOrderRespType;
import com.binance.client.model.enums.OrderSide;
import com.binance.client.model.enums.OrderType;
import com.binance.client.model.trade.AccountInformation;
import com.binance.client.model.trade.Order;
import com.furiousTidy.magicbean.apiproxy.ProxyUtil;
import com.furiousTidy.magicbean.apiproxy.SpotSyncClientProxy;
import com.furiousTidy.magicbean.config.BeanConfig;
import com.furiousTidy.magicbean.dbutil.dao.PairsTradeDao;
import com.furiousTidy.magicbean.dbutil.dao.TradeInfoDao;
import com.furiousTidy.magicbean.dbutil.model.PairsTradeModel;
import com.furiousTidy.magicbean.subscription.PreTradeService;
import com.furiousTidy.magicbean.util.BeanConstant;
import com.furiousTidy.magicbean.util.BinanceClient;
import com.furiousTidy.magicbean.util.MarketCache;
import com.furiousTidy.magicbean.util.TradeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

//import com.furiousTidy.magicbean.influxdb.InfluxDbConnection;


// schedule service
@Configuration
@Slf4j
public class MarketScheduleService {

    @Autowired
   MarketService marketService;

    @Scheduled(cron = "0 0 8,16,20 * * ?")
    public void get24Volume(){
        try {
            marketService.get24Volume();
        } catch (Exception e) {
            log.error("get24Volume_exception:" + e);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void getGlobalLongShortAccountRatio(){
        try {
            marketService.getGlobalLongShortAccountRatio();
        } catch (Exception e) {
            log.error("getGlobalLongShortAccountRatio_exception:" + e);
        }
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void calAndStoreDepth(){
        try {
            marketService.calculateDepth();
        } catch (Exception e) {
            log.error("calAndStoreDepth_exception:" + e);

        }
    }

    @Scheduled(cron = "0 0 0/1 * * ?")
    public void getAndStoreDvol(){
        try {
            marketService.getDvol();
        } catch (Exception e) {
            log.error("getAndStoreDvol_exception:" + e);
        }
    }


    //alert interest
    @Scheduled(cron = "0 0 0/4 * * ?")
    public void alertOpenInterest(){
        try {
            marketService.alertOpenInterest();
        } catch (Exception e) {
            log.error("alertOpenInterest_exception:" + e);
        }
    }

    //k line
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void getAndStoreAllKLine()  {
        try {
            marketService.getAndStoreAllKLine();
        } catch (InterruptedException e) {
            log.error("getAndStoreAllKLine_exception:" + e);
        }
    }

    public static void main(String[] args){

    }





}
