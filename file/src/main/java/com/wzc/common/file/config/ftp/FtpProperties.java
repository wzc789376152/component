package com.wzc.common.file.config.ftp;

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
     * 保存文件夹
     */
    protected String workDir = "temp";
    /**
     * 编码格式
     */
    protected String encoding;
    /**
     * 远程ftp服务器根目录
     */
    protected String root = "/";
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


    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
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

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
}