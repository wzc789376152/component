package com.github.wzc789376152.file;

public class SmbProperties {
    /**
     * 服务器路径
     */
    protected String url;
    /**
     * 访问用户名
     */
    protected String username;
    /**
     * 访问密码
     */
    protected String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
