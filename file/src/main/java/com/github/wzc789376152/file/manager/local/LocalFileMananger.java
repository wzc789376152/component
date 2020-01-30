package com.github.wzc789376152.file.manager.local;

import com.github.wzc789376152.file.utils.FilePathUtils;
import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.config.local.LocalProperties;
import com.github.wzc789376152.file.manager.IFileManager;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LocalFileMananger implements IFileManager {
    private LocalProperties localProperties;

    public LocalFileMananger(LocalProperties localProperties) {
        this.localProperties = localProperties;
    }

    @Override
    public void init(FileProperties fileProperties) {
        if (localProperties == null) {
            localProperties = new LocalProperties();
        }
        //创建服务器文件夹；需要服务器权限
        localProperties.setWorkDir(System.getProperty("user.dir") + (fileProperties.getProject() == null ? "" : (File.separator + fileProperties.getProject())) + FilePathUtils.formatPath(localProperties.getWorkDir()));
        File file = new File(localProperties.getWorkDir());
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @Override
    public List<String> getAllFilesName() {
        File file1 = new File(localProperties.getWorkDir());
        File[] tempList1 = file1.listFiles();
        return Arrays.stream(tempList1).filter(file -> file.isFile()).map(File::getName).collect(Collectors.toList());
    }

    @Override
    public void upload(String filename, InputStream inputStream) throws IOException {
        File dest = new File(localProperties.getWorkDir() + filename);
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

    @Override
    public void download(String filename, OutputStream outputStream) throws IOException {
        InputStream inputStream;
        File file = new File(localProperties.getWorkDir() + filename);
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

    @Override
    public void delete(String filename) throws IOException {
        File file = new File(localProperties.getWorkDir() + filename);
        if (file.exists()) {
            file.delete();
        }
    }
}
