package com.github.wzc789376152.vo;

public class PageVo {
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }
}