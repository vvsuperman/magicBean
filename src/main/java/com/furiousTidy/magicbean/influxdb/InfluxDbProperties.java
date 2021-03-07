package com.furiousTidy.magicbean.influxdb;

public class InfluxDbProperties {

    private String url;
    private String userName;
    private String password;
    private String database;
    private String retentionPolicy = "autogen";
    private String retentionPolicyTime = "30d";
    private int actions = 2000;
    private int flushDuration = 1000;
    private int jitterDuration = 0;
    private int bufferLimit = 10000;

    public InfluxDbProperties() {
    }

    public String getUrl() {
        return this.url;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getPassword() {
        return this.password;
    }

    public String getDatabase() {
        return this.database;
    }

    public String getRetentionPolicy() {
        return this.retentionPolicy;
    }

    public String getRetentionPolicyTime() {
        return this.retentionPolicyTime;
    }

    public int getActions() {
        return this.actions;
    }

    public int getFlushDuration() {
        return this.flushDuration;
    }

    public int getJitterDuration() {
        return this.jitterDuration;
    }

    public int getBufferLimit() {
        return this.bufferLimit;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setRetentionPolicy(String retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public void setRetentionPolicyTime(String retentionPolicyTime) {
        this.retentionPolicyTime = retentionPolicyTime;
    }

    public void setActions(int actions) {
        this.actions = actions;
    }

    public void setFlushDuration(int flushDuration) {
        this.flushDuration = flushDuration;
    }

    public void setJitterDuration(int jitterDuration) {
        this.jitterDuration = jitterDuration;
    }

    public void setBufferLimit(int bufferLimit) {
        this.bufferLimit = bufferLimit;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof InfluxDbProperties)) return false;
        final InfluxDbProperties other = (InfluxDbProperties) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$url = this.getUrl();
        final Object other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) return false;
        final Object this$userName = this.getUserName();
        final Object other$userName = other.getUserName();
        if (this$userName == null ? other$userName != null : !this$userName.equals(other$userName)) return false;
        final Object this$password = this.getPassword();
        final Object other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
        final Object this$database = this.getDatabase();
        final Object other$database = other.getDatabase();
        if (this$database == null ? other$database != null : !this$database.equals(other$database)) return false;
        final Object this$retentionPolicy = this.getRetentionPolicy();
        final Object other$retentionPolicy = other.getRetentionPolicy();
        if (this$retentionPolicy == null ? other$retentionPolicy != null : !this$retentionPolicy.equals(other$retentionPolicy))
            return false;
        final Object this$retentionPolicyTime = this.getRetentionPolicyTime();
        final Object other$retentionPolicyTime = other.getRetentionPolicyTime();
        if (this$retentionPolicyTime == null ? other$retentionPolicyTime != null : !this$retentionPolicyTime.equals(other$retentionPolicyTime))
            return false;
        if (this.getActions() != other.getActions()) return false;
        if (this.getFlushDuration() != other.getFlushDuration()) return false;
        if (this.getJitterDuration() != other.getJitterDuration()) return false;
        if (this.getBufferLimit() != other.getBufferLimit()) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof InfluxDbProperties;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $url = this.getUrl();
        result = result * PRIME + ($url == null ? 43 : $url.hashCode());
        final Object $userName = this.getUserName();
        result = result * PRIME + ($userName == null ? 43 : $userName.hashCode());
        final Object $password = this.getPassword();
        result = result * PRIME + ($password == null ? 43 : $password.hashCode());
        final Object $database = this.getDatabase();
        result = result * PRIME + ($database == null ? 43 : $database.hashCode());
        final Object $retentionPolicy = this.getRetentionPolicy();
        result = result * PRIME + ($retentionPolicy == null ? 43 : $retentionPolicy.hashCode());
        final Object $retentionPolicyTime = this.getRetentionPolicyTime();
        result = result * PRIME + ($retentionPolicyTime == null ? 43 : $retentionPolicyTime.hashCode());
        result = result * PRIME + this.getActions();
        result = result * PRIME + this.getFlushDuration();
        result = result * PRIME + this.getJitterDuration();
        result = result * PRIME + this.getBufferLimit();
        return result;
    }

    public String toString() {
        return "InfluxDbProperties(url=" + this.getUrl() + ", userName=" + this.getUserName() + ", password=" + this.getPassword() + ", database=" + this.getDatabase() + ", retentionPolicy=" + this.getRetentionPolicy() + ", retentionPolicyTime=" + this.getRetentionPolicyTime() + ", actions=" + this.getActions() + ", flushDuration=" + this.getFlushDuration() + ", jitterDuration=" + this.getJitterDuration() + ", bufferLimit=" + this.getBufferLimit() + ")";
    }
}
