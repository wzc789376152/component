package com.github.wzc789376152.file.config.local;

public class LocalProperties {
    /**
     * 保存文件夹
     */
    private String workDir = "temp";
    /**
     * 编码格式
     */
    private String encoding = "UTF-8";

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
}
