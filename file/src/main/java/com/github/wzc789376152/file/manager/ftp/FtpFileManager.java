package com.github.wzc789376152.file.manager.ftp;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.config.ftp.FtpClientFactory;
import com.github.wzc789376152.file.config.ftp.FtpProperties;
import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.utils.FilePathUtils;
import com.github.wzc789376152.file.config.ftp.FtpPool;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FtpFileManager implements IFileManager {
    private FtpProperties ftpProperties;
    private FtpPool ftpPool;

    public FtpFileManager(FtpProperties ftpProperties) {
        this.ftpProperties = ftpProperties;
    }

    @Override
    public void init(FileProperties fileProperties) {
        if (ftpProperties == null) {
            throw new RuntimeException("未进行ftp配置");
        }
        ftpProperties.setWorkDir(FilePathUtils.formatPath(ftpProperties.getWorkDir()));
        FtpClientFactory ftpClientFactory = new FtpClientFactory(ftpProperties);
        ftpPool = new FtpPool(ftpClientFactory);
    }

    @Override
    public List<String> getAllFilesName() {
        FTPClient client = ftpPool.getFTPClient();
        try {
            return Arrays.stream(client.listFiles()).filter(ftpFile -> ftpFile.isFile()).map(FTPFile::getName).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ftpPool.returnFTPClient(client);//归还资源
        }
        return null;
    }

    @Override
    public void upload(String filename, InputStream inputStream) throws IOException {
        FTPClient ftpClient = ftpPool.getFTPClient();
        //开始进行文件上
        if (ftpClient.listFiles(filename).length == 0) {
            boolean result = ftpClient.storeFile(filename, inputStream);//执行文件传输
            if (!result) {//上传失败
                throw new IOException("远程上传失败");
            }
        }
        ftpPool.returnFTPClient(ftpClient);//归还资源
    }

    @Override
    public void download(String filename, OutputStream outputStream) throws IOException {
        FTPClient ftpClient = ftpPool.getFTPClient();
        //将文件直接读取到响应体中
        boolean is = ftpClient.retrieveFile(filename, outputStream);
        if (!is) {
            throw new IOException("文件不存在！");
        }
        ftpPool.returnFTPClient(ftpClient);
    }

    @Override
    public void delete(String filename) throws IOException {
        FTPClient ftpClient = ftpPool.getFTPClient();
        boolean is = ftpClient.deleteFile(filename);
        ftpPool.returnFTPClient(ftpClient);
        if (!is) {
            throw new IOException("ftp文件删除失败!");
        }
    }
}
