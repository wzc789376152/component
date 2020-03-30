package com.github.wzc789376152.file.manager;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.FtpClientFactory;
import com.github.wzc789376152.file.FtpPool;
import com.github.wzc789376152.file.FtpProperties;
import com.github.wzc789376152.file.utils.FilePathUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class FtpFileManagerAbstract implements IFileManager {
    Logger logger = Logger.getLogger(IFileManager.class.getName());
    protected FtpProperties ftpProperties;
    private FtpPool ftpPool;

    public abstract FtpProperties ftpProperties();

    public void init(FileProperties fileProperties) {
        logger.info("use ftpFileManager");
        if (ftpProperties == null) {
            ftpProperties = ftpProperties();
            if (ftpProperties == null) {
                throw new RuntimeException("未进行ftp配置");
            }
        }
        fileProperties.setWorkDir(FilePathUtils.formatPath(fileProperties.getWorkDir()));
        FtpClientFactory ftpClientFactory = new FtpClientFactory(ftpProperties);
        ftpPool = new FtpPool(ftpClientFactory);
        FTPClient ftpClient = ftpPool.getFTPClient();
        if (fileProperties.getWorkDir().endsWith(File.separator)) {
            fileProperties.setWorkDir(fileProperties.getWorkDir().substring(0, fileProperties.getWorkDir().length() - 1));
        }
        fileProperties.setWorkDir(fileProperties.getWorkDir().replace(File.separator, "/"));
        //初始化保存路径
        try {
            if (!ftpClient.changeWorkingDirectory(fileProperties.getWorkDir())) {
                if (ftpClient.makeDirectory(fileProperties.getWorkDir())) {
                    ftpClient.changeWorkingDirectory(fileProperties.getWorkDir());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            ftpPool.returnFTPClient(ftpClient);
        }
    }

    public List<String> getAllFilesName() {
        FTPClient client = ftpPool.getFTPClient();
        List<String> result = new ArrayList<String>();
        try {
            for (FTPFile file : client.listFiles()) {
                if (file.isFile()) {
                    result.add(file.getName());
                }
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ftpPool.returnFTPClient(client);//归还资源
        }
        return null;
    }

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

    public void download(String filename, OutputStream outputStream) throws IOException {
        FTPClient ftpClient = ftpPool.getFTPClient();
        //将文件直接读取到响应体中
        boolean is = ftpClient.retrieveFile(filename, outputStream);
        if (!is) {
            throw new IOException("文件不存在！");
        }
        ftpPool.returnFTPClient(ftpClient);
    }

    public void delete(String filename) throws IOException {
        FTPClient ftpClient = ftpPool.getFTPClient();
        boolean is = ftpClient.deleteFile(filename);
        ftpPool.returnFTPClient(ftpClient);
        if (!is) {
            throw new IOException("ftp文件删除失败!");
        }
    }

    public void changeWorkDir(String filepath) throws IOException {
        FTPClient ftpClient = ftpPool.getFTPClient();
        if (filepath.endsWith(File.separator)) {
            filepath = filepath.substring(0, filepath.length() - 1);
        }
        filepath = filepath.replace(File.separator, "/");
        //初始化保存路径
        try {
            if (!ftpClient.changeWorkingDirectory(filepath)) {
                if (ftpClient.makeDirectory(filepath)) {
                    ftpClient.changeWorkingDirectory(filepath);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            ftpPool.returnFTPClient(ftpClient);
        }
    }

    public void setFtpProperties(FtpProperties ftpProperties) {
        this.ftpProperties = ftpProperties;
    }
}
