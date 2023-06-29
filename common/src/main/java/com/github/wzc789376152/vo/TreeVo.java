package com.github.wzc789376152.vo;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;



public class TreeVo<T> {
    private Serializable id;
    private Serializable parentId;
    private Collection<T> childList;

    public static <T extends TreeVo<T>> List<T> getTreeModel(Collection<T> treeModels) {
        return findParent(treeModels);
    }

    public static <T extends TreeVo<T>> List<T> findChild(Serializable parentId, Collection<T> treeModels) {
        return treeModels.stream().filter(i -> i.getParentId() != null && i.getParentId().equals(parentId)).peek(i -> i.setChildList(findChild(i.getId(), treeModels))).collect(Collectors.toList());
    }

    private static <T extends TreeVo<T>> List<T> findParent(Collection<T> treeModels) {
        return treeModels.stream().filter(i -> i.getParentId() == null || treeModels.stream().noneMatch(j -> j.getId().equals(i.getParentId()))).peek(i -> i.setChildList(findChild(i.getId(), treeModels))).collect(Collectors.toList());
    }


    public Serializable getParentId() {
        return parentId;
    }

    public void setParentId(Serializable parentId) {
        this.parentId = parentId;
    }

    public Serializable getId() {
        return id;
    }

    public void setId(Serializable id) {
        this.id = id;
    }

    public void setChildList(Collection<T> childList) {
        this.childList = childList;
    }

    public Collection<T> getChildList() {
        return childList;
    }
}
