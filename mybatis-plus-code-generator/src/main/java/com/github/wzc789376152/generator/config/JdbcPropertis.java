package com.github.wzc789376152.generator.config;

public class JdbcPropertis {
    private String type;
    private String url;
    private String username;
    private String password;
    private String driverName;

    public JdbcPropertis() {
    }

    public JdbcPropertis(String type, String url, String username, String password, String driverName) {
        this.type = type;
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverName = driverName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
}
