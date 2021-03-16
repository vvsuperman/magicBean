package com.furiousTidy.magicbean.dbutil1;

import java.util.ArrayList;
import java.util.List;

public class PairsTradeHisExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public PairsTradeHisExample() {
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

        public Criteria andOpenidIsNull() {
            addCriterion("openId is null");
            return (Criteria) this;
        }

        public Criteria andOpenidIsNotNull() {
            addCriterion("openId is not null");
            return (Criteria) this;
        }

        public Criteria andOpenidEqualTo(String value) {
            addCriterion("openId =", value, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidNotEqualTo(String value) {
            addCriterion("openId <>", value, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidGreaterThan(String value) {
            addCriterion("openId >", value, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidGreaterThanOrEqualTo(String value) {
            addCriterion("openId >=", value, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidLessThan(String value) {
            addCriterion("openId <", value, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidLessThanOrEqualTo(String value) {
            addCriterion("openId <=", value, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidLike(String value) {
            addCriterion("openId like", value, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidNotLike(String value) {
            addCriterion("openId not like", value, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidIn(List<String> values) {
            addCriterion("openId in", values, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidNotIn(List<String> values) {
            addCriterion("openId not in", values, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidBetween(String value1, String value2) {
            addCriterion("openId between", value1, value2, "openid");
            return (Criteria) this;
        }

        public Criteria andOpenidNotBetween(String value1, String value2) {
            addCriterion("openId not between", value1, value2, "openid");
            return (Criteria) this;
        }

        public Criteria andCloseidIsNull() {
            addCriterion("closeId is null");
            return (Criteria) this;
        }

        public Criteria andCloseidIsNotNull() {
            addCriterion("closeId is not null");
            return (Criteria) this;
        }

        public Criteria andCloseidEqualTo(String value) {
            addCriterion("closeId =", value, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidNotEqualTo(String value) {
            addCriterion("closeId <>", value, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidGreaterThan(String value) {
            addCriterion("closeId >", value, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidGreaterThanOrEqualTo(String value) {
            addCriterion("closeId >=", value, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidLessThan(String value) {
            addCriterion("closeId <", value, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidLessThanOrEqualTo(String value) {
            addCriterion("closeId <=", value, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidLike(String value) {
            addCriterion("closeId like", value, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidNotLike(String value) {
            addCriterion("closeId not like", value, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidIn(List<String> values) {
            addCriterion("closeId in", values, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidNotIn(List<String> values) {
            addCriterion("closeId not in", values, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidBetween(String value1, String value2) {
            addCriterion("closeId between", value1, value2, "closeid");
            return (Criteria) this;
        }

        public Criteria andCloseidNotBetween(String value1, String value2) {
            addCriterion("closeId not between", value1, value2, "closeid");
            return (Criteria) this;
        }

        public Criteria andOpenratioIsNull() {
            addCriterion("openRatio is null");
            return (Criteria) this;
        }

        public Criteria andOpenratioIsNotNull() {
            addCriterion("openRatio is not null");
            return (Criteria) this;
        }

        public Criteria andOpenratioEqualTo(Double value) {
            addCriterion("openRatio =", value, "openratio");
            return (Criteria) this;
        }

        public Criteria andOpenratioNotEqualTo(Double value) {
            addCriterion("openRatio <>", value, "openratio");
            return (Criteria) this;
        }

        public Criteria andOpenratioGreaterThan(Double value) {
            addCriterion("openRatio >", value, "openratio");
            return (Criteria) this;
        }

        public Criteria andOpenratioGreaterThanOrEqualTo(Double value) {
            addCriterion("openRatio >=", value, "openratio");
            return (Criteria) this;
        }

        public Criteria andOpenratioLessThan(Double value) {
            addCriterion("openRatio <", value, "openratio");
            return (Criteria) this;
        }

        public Criteria andOpenratioLessThanOrEqualTo(Double value) {
            addCriterion("openRatio <=", value, "openratio");
            return (Criteria) this;
        }

        public Criteria andOpenratioIn(List<Double> values) {
            addCriterion("openRatio in", values, "openratio");
            return (Criteria) this;
        }

        public Criteria andOpenratioNotIn(List<Double> values) {
            addCriterion("openRatio not in", values, "openratio");
            return (Criteria) this;
        }

        public Criteria andOpenratioBetween(Double value1, Double value2) {
            addCriterion("openRatio between", value1, value2, "openratio");
            return (Criteria) this;
        }

        public Criteria andOpenratioNotBetween(Double value1, Double value2) {
            addCriterion("openRatio not between", value1, value2, "openratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioIsNull() {
            addCriterion("closeRatio is null");
            return (Criteria) this;
        }

        public Criteria andCloseratioIsNotNull() {
            addCriterion("closeRatio is not null");
            return (Criteria) this;
        }

        public Criteria andCloseratioEqualTo(Double value) {
            addCriterion("closeRatio =", value, "closeratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioNotEqualTo(Double value) {
            addCriterion("closeRatio <>", value, "closeratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioGreaterThan(Double value) {
            addCriterion("closeRatio >", value, "closeratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioGreaterThanOrEqualTo(Double value) {
            addCriterion("closeRatio >=", value, "closeratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioLessThan(Double value) {
            addCriterion("closeRatio <", value, "closeratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioLessThanOrEqualTo(Double value) {
            addCriterion("closeRatio <=", value, "closeratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioIn(List<Double> values) {
            addCriterion("closeRatio in", values, "closeratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioNotIn(List<Double> values) {
            addCriterion("closeRatio not in", values, "closeratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioBetween(Double value1, Double value2) {
            addCriterion("closeRatio between", value1, value2, "closeratio");
            return (Criteria) this;
        }

        public Criteria andCloseratioNotBetween(Double value1, Double value2) {
            addCriterion("closeRatio not between", value1, value2, "closeratio");
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