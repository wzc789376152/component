package com.github.wzc789376152.file.config.ftp;

public class FtpProperties {
    /**
     * ftp服务器路径
     */
    protected String host;
    /**
     * ftp服务器端口
     */
    protected int port = 21;
    /**
     * ftp用户名
     */
    protected String username;
    /**
     * ftp密码
     */
    protected String password;
    /**
     * 编码格式
     */
    protected String encoding;
    /**
     * 最大连接数
     */
    protected int maxTotal = 10;
    /**
     * 最小空闲连接
     */
    protected int minIdel = 1;
    /**
     * 最大空闲连接
     */
    protected int maxIdle = 8;
    /**
     * 请求连接最大等待时间(毫秒)
     */
    protected int maxWaitMillis = 3000;
    /**
     * 是否启用
     */
    private boolean enable;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMinIdel() {
        return minIdel;
    }

    public void setMinIdel(int minIdel) {
        this.minIdel = minIdel;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMaxWaitMillis() {
        return maxWaitMillis;
    }

    public void setMaxWaitMillis(int maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}