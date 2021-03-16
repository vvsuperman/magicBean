package com.furiousTidy.magicbean.dbutil1;

import java.util.ArrayList;
import java.util.List;

public class TradeInfoExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public TradeInfoExample() {
        oredCriteria = new ArrayList<>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Integer value) {
            addCriterion("id =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Integer value) {
            addCriterion("id <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Integer value) {
            addCriterion("id >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("id >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Integer value) {
            addCriterion("id <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Integer value) {
            addCriterion("id <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Integer> values) {
            addCriterion("id in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Integer> values) {
            addCriterion("id not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Integer value1, Integer value2) {
            addCriterion("id between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Integer value1, Integer value2) {
            addCriterion("id not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andSymbolIsNull() {
            addCriterion("symbol is null");
            return (Criteria) this;
        }

        public Criteria andSymbolIsNotNull() {
            addCriterion("symbol is not null");
            return (Criteria) this;
        }

        public Criteria andSymbolEqualTo(String value) {
            addCriterion("symbol =", value, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolNotEqualTo(String value) {
            addCriterion("symbol <>", value, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolGreaterThan(String value) {
            addCriterion("symbol >", value, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolGreaterThanOrEqualTo(String value) {
            addCriterion("symbol >=", value, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolLessThan(String value) {
            addCriterion("symbol <", value, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolLessThanOrEqualTo(String value) {
            addCriterion("symbol <=", value, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolLike(String value) {
            addCriterion("symbol like", value, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolNotLike(String value) {
            addCriterion("symbol not like", value, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolIn(List<String> values) {
            addCriterion("symbol in", values, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolNotIn(List<String> values) {
            addCriterion("symbol not in", values, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolBetween(String value1, String value2) {
            addCriterion("symbol between", value1, value2, "symbol");
            return (Criteria) this;
        }

        public Criteria andSymbolNotBetween(String value1, String value2) {
            addCriterion("symbol not between", value1, value2, "symbol");
            return (Criteria) this;
        }

        public Criteria andFuturepriceIsNull() {
            addCriterion("futurePrice is null");
            return (Criteria) this;
        }

        public Criteria andFuturepriceIsNotNull() {
            addCriterion("futurePrice is not null");
            return (Criteria) this;
        }

        public Criteria andFuturepriceEqualTo(Double value) {
            addCriterion("futurePrice =", value, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFuturepriceNotEqualTo(Double value) {
            addCriterion("futurePrice <>", value, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFuturepriceGreaterThan(Double value) {
            addCriterion("futurePrice >", value, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFuturepriceGreaterThanOrEqualTo(Double value) {
            addCriterion("futurePrice >=", value, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFuturepriceLessThan(Double value) {
            addCriterion("futurePrice <", value, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFuturepriceLessThanOrEqualTo(Double value) {
            addCriterion("futurePrice <=", value, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFuturepriceIn(List<Double> values) {
            addCriterion("futurePrice in", values, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFuturepriceNotIn(List<Double> values) {
            addCriterion("futurePrice not in", values, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFuturepriceBetween(Double value1, Double value2) {
            addCriterion("futurePrice between", value1, value2, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFuturepriceNotBetween(Double value1, Double value2) {
            addCriterion("futurePrice not between", value1, value2, "futureprice");
            return (Criteria) this;
        }

        public Criteria andFutureqtyIsNull() {
            addCriterion("futureQty is null");
            return (Criteria) this;
        }

        public Criteria andFutureqtyIsNotNull() {
            addCriterion("futureQty is not null");
            return (Criteria) this;
        }

        public Criteria andFutureqtyEqualTo(Double value) {
            addCriterion("futureQty =", value, "futureqty");
            return (Criteria) this;
        }

        public Criteria andFutureqtyNotEqualTo(Double value) {
            addCriterion("futureQty <>", value, "futureqty");
            return (Criteria) this;
        }

        public Criteria andFutureqtyGreaterThan(Double value) {
            addCriterion("futureQty >", value, "futureqty");
            return (Criteria) this;
        }

        public Criteria andFutureqtyGreaterThanOrEqualTo(Double value) {
            addCriterion("futureQty >=", value, "futureqty");
            return (Criteria) this;
        }

        public Criteria andFutureqtyLessThan(Double value) {
            addCriterion("futureQty <", value, "futureqty");
            return (Criteria) this;
        }

        public Criteria andFutureqtyLessThanOrEqualTo(Double value) {
            addCriterion("futureQty <=", value, "futureqty");
            return (Criteria) this;
        }

        public Criteria andFutureqtyIn(List<Double> values) {
            addCriterion("futureQty in", values, "futureqty");
            return (Criteria) this;
        }

        public Criteria andFutureqtyNotIn(List<Double> values) {
            addCriterion("futureQty not in", values, "futureqty");
            return (Criteria) this;
        }

        public Criteria andFutureqtyBetween(Double value1, Double value2) {
            addCriterion("futureQty between", value1, value2, "futureqty");
            return (Criteria) this;
        }

        public Criteria andFutureqtyNotBetween(Double value1, Double value2) {
            addCriterion("futureQty not between", value1, value2, "futureqty");
            return (Criteria) this;
        }

        public Criteria andCreatetimeIsNull() {
            addCriterion("createTime is null");
            return (Criteria) this;
        }

        public Criteria andCreatetimeIsNotNull() {
            addCriterion("createTime is not null");
            return (Criteria) this;
        }

        public Criteria andCreatetimeEqualTo(String value) {
            addCriterion("createTime =", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeNotEqualTo(String value) {
            addCriterion("createTime <>", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeGreaterThan(String value) {
            addCriterion("createTime >", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeGreaterThanOrEqualTo(String value) {
            addCriterion("createTime >=", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeLessThan(String value) {
            addCriterion("createTime <", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeLessThanOrEqualTo(String value) {
            addCriterion("createTime <=", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeLike(String value) {
            addCriterion("createTime like", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeNotLike(String value) {
            addCriterion("createTime not like", value, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeIn(List<String> values) {
            addCriterion("createTime in", values, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeNotIn(List<String> values) {
            addCriterion("createTime not in", values, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeBetween(String value1, String value2) {
            addCriterion("createTime between", value1, value2, "createtime");
            return (Criteria) this;
        }

        public Criteria andCreatetimeNotBetween(String value1, String value2) {
            addCriterion("createTime not between", value1, value2, "createtime");
            return (Criteria) this;
        }

        public Criteria andSpotpriceIsNull() {
            addCriterion("spotPrice is null");
            return (Criteria) this;
        }

        public Criteria andSpotpriceIsNotNull() {
            addCriterion("spotPrice is not null");
            return (Criteria) this;
        }

        public Criteria andSpotpriceEqualTo(Double value) {
            addCriterion("spotPrice =", value, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotpriceNotEqualTo(Double value) {
            addCriterion("spotPrice <>", value, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotpriceGreaterThan(Double value) {
            addCriterion("spotPrice >", value, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotpriceGreaterThanOrEqualTo(Double value) {
            addCriterion("spotPrice >=", value, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotpriceLessThan(Double value) {
            addCriterion("spotPrice <", value, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotpriceLessThanOrEqualTo(Double value) {
            addCriterion("spotPrice <=", value, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotpriceIn(List<Double> values) {
            addCriterion("spotPrice in", values, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotpriceNotIn(List<Double> values) {
            addCriterion("spotPrice not in", values, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotpriceBetween(Double value1, Double value2) {
            addCriterion("spotPrice between", value1, value2, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotpriceNotBetween(Double value1, Double value2) {
            addCriterion("spotPrice not between", value1, value2, "spotprice");
            return (Criteria) this;
        }

        public Criteria andSpotqtyIsNull() {
            addCriterion("spotQty is null");
            return (Criteria) this;
        }

        public Criteria andSpotqtyIsNotNull() {
            addCriterion("spotQty is not null");
            return (Criteria) this;
        }

        public Criteria andSpotqtyEqualTo(Double value) {
            addCriterion("spotQty =", value, "spotqty");
            return (Criteria) this;
        }

        public Criteria andSpotqtyNotEqualTo(Double value) {
            addCriterion("spotQty <>", value, "spotqty");
            return (Criteria) this;
        }

        public Criteria andSpotqtyGreaterThan(Double value) {
            addCriterion("spotQty >", value, "spotqty");
            return (Criteria) this;
        }

        public Criteria andSpotqtyGreaterThanOrEqualTo(Double value) {
            addCriterion("spotQty >=", value, "spotqty");
            return (Criteria) this;
        }

        public Criteria andSpotqtyLessThan(Double value) {
            addCriterion("spotQty <", value, "spotqty");
            return (Criteria) this;
        }

        public Criteria andSpotqtyLessThanOrEqualTo(Double value) {
            addCriterion("spotQty <=", value, "spotqty");
            return (Criteria) this;
        }

        public Criteria andSpotqtyIn(List<Double> values) {
            addCriterion("spotQty in", values, "spotqty");
            return (Criteria) this;
        }

        public Criteria andSpotqtyNotIn(List<Double> values) {
            addCriterion("spotQty not in", values, "spotqty");
            return (Criteria) this;
        }

        public Criteria andSpotqtyBetween(Double value1, Double value2) {
            addCriterion("spotQty between", value1, value2, "spotqty");
            return (Criteria) this;
        }

        public Criteria andSpotqtyNotBetween(Double value1, Double value2) {
            addCriterion("spotQty not between", value1, value2, "spotqty");
            return (Criteria) this;
        }
    }

    public static class Criteria extends GeneratedCriteria {
        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}