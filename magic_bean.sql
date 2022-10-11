 CREATE TABLE pairs_trade
(
id varchar(30),
openId varchar(30),
closeId varchar(30),
openRatio varchar(30),
closeRatio varchar(30),
profit varchar(30),
createTime varchar(30),
updateTime varchar(30),
origOpenRatio varchar(30),
origCloseRatio varchar(30)
);


CREATE TABLE trade_info
(
symbol varchar(30),
orderId varchar(30),
futurePrice varchar(30),
futureQty varchar(30),
spotPrice varchar(30),
spotQty varchar(30),
createTime varchar(30),
updateTime varchar(30),
futureTickDelayTime varchar(30),
spotTickDelayTime varchar(30)
);

create table trade_order
(
symbol varchar(30),
clientOrderId varchar(30),
type varchar(30)
);

