package com.github.wzc789376152.file.manager;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.utils.FilePathUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class LocalFileManangerAbstract implements IFileManager {
    Logger logger = Logger.getLogger(IFileManager.class.getName());
    private FileProperties fileProperties;

    public void init(FileProperties fileProperties) {
        logger.info("use localFileManager");
        //创建服务器文件夹；需要服务器权限
        fileProperties.setWorkDir(System.getProperty("user.dir") + (fileProperties.getProject() == null ? "" : (File.separator + fileProperties.getProject())) + FilePathUtils.formatPath(fileProperties.getWorkDir()));
        File file = new File(fileProperties.getWorkDir());
        if (!file.exists()) {
            file.mkdirs();
        }
        this.fileProperties = fileProperties;
    }

    public List<String> getAllFilesName() {
        File file1 = new File(fileProperties.getWorkDir());
        File[] tempList1 = file1.listFiles();
        List<String> result = new ArrayList<String>();
        for (File file : tempList1) {
            result.add(file.getName());
        }
        return result;
    }

    public void upload(String filename, InputStream inputStream) throws IOException {
        File dest = new File(fileProperties.getWorkDir() + filename);
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = ((FileInputStream) inputStream).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }

    public void download(String filename, OutputStream outputStream) throws IOException {
        InputStream inputStream;
        File file = new File(fileProperties.getWorkDir() + filename);
        if (!file.exists()) {
            throw new IOException("文件不存在！");
        }
        inputStream = new FileInputStream(file);
        BufferedInputStream br = new BufferedInputStream(inputStream);
        byte[] buf = new byte[1024];
        int len = 0;
        while ((len = br.read(buf)) > 0) {
            outputStream.write(buf, 0, len);
        }
        br.close();
        outputStream.close();
        inputStream.close();
    }

    public void delete(String filename) throws IOException {
        File file = new File(fileProperties.getWorkDir() + filename);
        if (file.exists()) {
            file.delete();
        }
    }

    public void changeWorkDir(String filepath) throws IOException {
        fileProperties.setWorkDir(System.getProperty("user.dir") + (fileProperties.getProject() == null ? "" : (File.separator + fileProperties.getProject())) + FilePathUtils.formatPath(filepath));
        File file = new File(fileProperties.getWorkDir());
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public String getDownloadUrl(String filename) {
        return filename;
    }
}
