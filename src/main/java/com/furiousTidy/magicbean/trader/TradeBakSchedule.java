//package com.furiousTidy.magicbean.trader;
//
//import com.furiousTidy.magicbean.dbutil.PairsTradeDao;
//import com.furiousTidy.magicbean.dbutil.PairsTradeModel;
//import com.furiousTidy.magicbean.dbutil.TradeInfoDao;
//import com.furiousTidy.magicbean.dbutil.TradeInfoModel;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.List;
//
//// schedule move tradeInfo and pairsTrade to his_table
//public class TradeBakSchedule {
//
//    @Autowired
//    public PairsTradeDao pairsTradeDao;
//
//    @Autowired
//    public TradeInfoDao tradeInfoDao;
//
//    // if tradeinfo has closeratio and not change between 10s
//    public void bakTradeInfo() throws InterruptedException {
//        List<PairsTradeModel> pairsTradeList = pairsTradeDao.findAllPairsTrade();
//        Thread.sleep(10000);
//        List<PairsTradeModel> pairsTradeList10 = pairsTradeDao.findAllPairsTrade();
//        for(PairsTradeModel pairsTrade: pairsTradeList){
//            for(PairsTradeModel pairsTrade10: pairsTradeList10){
//                if(pairsTrade.getId().equals(pairsTrade10.getId())
//                        && pairsTrade.getCloseRatio()!=null
//                        && pairsTrade.getCloseRatio().equals(pairsTrade10.getCloseRatio())){
//                    pairsTradeHisDao.insertPairsTrade(pairsTrade);
//                    pairsTradeDao.deletePairsTrade(pairsTrade);
//                    TradeInfoModel tradeInfo = tradeInfoDao.getTradeInfoByOpenId(pairsTrade.getOpenId());
//                    tradeInfoHisDao.insertTradeInfo(tradeInfo);
//                    tradeInfoDao.deleteTradeInfo(tradeInfo);
//                }
//            }
//        }
//
//    }
//
//}
