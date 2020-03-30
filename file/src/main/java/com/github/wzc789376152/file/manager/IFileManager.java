package com.github.wzc789376152.file.manager;

import com.github.wzc789376152.file.FileProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 文件处理器拓展接口，目前仅实现本地local及ftp服务器两种方式
 * 实现该接口可对文件处理类型进行拓展
 */
public interface IFileManager {
    /**
     * 初始化方法
     *
     * @param fileProperties 文件配置
     */
    void init(FileProperties fileProperties);

    /**
     * 获取所有文件的文件名
     *
     * @return 文件名集合
     */
    List<String> getAllFilesName();

    /**
     * 文件上传
     *
     * @param filename    文件名
     * @param inputStream 输入文件stream
     * @throws IOException 抛出文件异常
     */
    void upload(String filename, InputStream inputStream) throws IOException;

    /**
     * 文件下载
     *
     * @param filename     文件名
     * @param outputStream 输出文件stream
     * @throws IOException 抛出文件异常
     */
    void download(String filename, OutputStream outputStream) throws IOException;

    /**
     * 文件删除
     *
     * @param filename 文件名
     * @throws IOException 抛出文件异常
     */
    void delete(String filename) throws IOException;

    /**
     * 修改文件保存地址
     *
     * @param filepath
     * @throws IOException
     */
    void changeWorkDir(String filepath) throws IOException;
}
