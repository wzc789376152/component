package com.github.wzc789376152.file.service;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.manager.IFileManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IFileService {

    /**
     * 获取文件配置信息
     *
     * @return 返回文件配置
     */
    FileProperties getProperties();

    /**
     * 获取文件管理器
     * @return 返回文件处理器
     */
    IFileManager getFileManager();

    /**
     * 获取保存的文件列表
     *
     * @return
     * @throws IOException 抛出异常
     */
    List<String> getFileNameList() throws IOException;

    /**
     * 上传临时文件
     *
     * @param inputStream 输入文件stream
     * @param filename 文件名
     * @throws IOException 抛出异常
     */
    void uploadCache(InputStream inputStream, String filename) throws IOException;

    /**
     * 将临时文件持久化
     *
     * @param filename 文件名
     * @throws IOException 抛出异常
     */
    void submit(String filename) throws IOException;

    /**
     * 下载文件
     *
     * @param fileName 文件名
     * @param outputStream 输出文件stream
     * @throws IOException 抛出异常
     */
    void download(String fileName, OutputStream outputStream) throws IOException;

    /**
     * 删除文件
     *
     * @param filename 文件名
     * @throws IOException 抛出异常
     */
    void delete(String filename) throws IOException;
}
