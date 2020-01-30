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
     * @return
     */
    FileProperties getProperties();

    /**
     * 获取文件管理器
     * @return
     */
    IFileManager getFileManager();

    /**
     * 获取保存的文件列表
     *
     * @return
     * @throws IOException
     */
    List<String> getFileNameList() throws IOException;

    /**
     * 上传临时文件
     *
     * @param inputStream
     * @param filename
     * @throws IOException
     */
    void uploadCache(InputStream inputStream, String filename) throws IOException;

    /**
     * 将临时文件持久化
     *
     * @param filename
     * @throws IOException
     */
    void submit(String filename) throws IOException;

    /**
     * 下载文件
     *
     * @param fileName
     * @param outputStream
     * @throws IOException
     */
    void download(String fileName, OutputStream outputStream) throws IOException;

    /**
     * 删除文件
     *
     * @param filename
     * @throws IOException
     */
    void delete(String filename) throws IOException;
}
