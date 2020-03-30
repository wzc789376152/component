package com.github.wzc789376152.file.service;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.manager.IFileManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface IFileService {

    /**
     * 获取临时文件夹路径
     *
     * @return 返回临时文件夹路径
     */
    String getTemporaryDir();

    /**
     * 获取缓存文件夹路径
     *
     * @return 返回缓存文件夹路径
     */
    String getCacheDir();

    /**
     * 获取文件配置信息
     *
     * @return 返回文件配置
     */
    FileProperties getProperties();

    /**
     * 获取文件管理器
     *
     * @return 返回文件处理器
     */
    IFileManager getFileManager();

    /**
     * 获取保存的文件列表
     *
     * @return 返回文件名集合
     * @throws IOException 抛出异常
     */
    List<String> getFileNameList() throws IOException;

    /**
     * 获取文件上传进度位置
     *
     * @param filename 文件名
     * @param token 文件标识
     * @return 位置
     * @throws IOException 异常
     */
    Long getFilePosition(String filename, String token) throws IOException;

    /**
     * 上传临时文件(整文件)
     *
     * @param inputStream 输入文件stream
     * @param filename    文件名
     * @throws IOException 抛出异常
     */
    void uploadCache(InputStream inputStream, String filename) throws IOException;

    /**
     * 上传临时文件(分片文件)
     *
     * @param inputStream 输入文件stream
     * @param filename 文件名
     * @param token 文件标识
     * @param position 位置
     * @throws IOException 抛出异常
     */
    void uploadCache(InputStream inputStream, String filename, String token, Long position) throws IOException;

    /**
     * 将临时文件持久化(整文件)
     *
     * @param filename 文件名
     * @throws IOException 抛出异常
     */
    void submit(String filename) throws IOException;

    /**
     * 将临时文件持久化（分片文件）
     *
     * @param filename 文件名
     * @param token    文件标识
     * @throws IOException 抛出异常
     */
    void submit(String filename, String token) throws IOException;

    /**
     * 下载文件
     *
     * @param fileName     文件名
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
