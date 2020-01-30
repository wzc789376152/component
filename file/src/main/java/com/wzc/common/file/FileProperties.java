package com.wzc.common.file;

/**
 * 文件属性
 * 可继承该类，对其进行拓展
 */
public class FileProperties {
    private String type = "local";//文件处理方式
    private Integer taskStartTime = 4;//临时文件清理时间：0-24小时;-1 立即开始
    private Integer taskPeriod = 1;//临时文件清理周期
    private String taskUnit = "day";//临时文件清理周期单位，1、year：每n年；2、month：每n月；3、day：每n天，默认为每天清理
    private String project;//项目名，用来区分保存文件的项目文件夹
    private Boolean isCache = false;//是否使用文件缓存，默认关闭，使用缓存前提是使用临时文件
    private Boolean isTemporary = false;//是否使用临时文件，默认关闭

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public Boolean getCache() {
        return isCache;
    }

    public void setCache(Boolean cache) {
        isCache = cache;
    }

    public Boolean getTemporary() {
        return isTemporary;
    }

    public void setTemporary(Boolean temporary) {
        isTemporary = temporary;
    }

    public Integer getTaskStartTime() {
        return taskStartTime;
    }

    public void setTaskStartTime(Integer taskStartTime) {
        this.taskStartTime = taskStartTime;
    }

    public Integer getTaskPeriod() {
        return taskPeriod;
    }

    public void setTaskPeriod(Integer taskPeriod) {
        this.taskPeriod = taskPeriod;
    }

    public String getTaskUnit() {
        return taskUnit;
    }

    public void setTaskUnit(String taskUnit) {
        this.taskUnit = taskUnit;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
