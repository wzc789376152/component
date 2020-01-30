package com.wzc.common.file.manager;

import com.wzc.common.file.FileProperties;

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
     * @param fileProperties
     */
    void init(FileProperties fileProperties);

    /**
     * 获取所有文件的文件名
     *
     * @return
     */
    List<String> getAllFilesName();

    /**
     * 文件上传
     *
     * @param filename
     * @param inputStream
     * @throws IOException
     */
    void upload(String filename, InputStream inputStream) throws IOException;

    /**
     * 文件下载
     *
     * @param filename
     * @param outputStream
     * @throws IOException
     */
    void download(String filename, OutputStream outputStream) throws IOException;

    /**
     * 文件删除
     *
     * @param filename
     * @throws IOException
     */
    void delete(String filename) throws IOException;
}
