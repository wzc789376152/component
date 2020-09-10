package com.github.wzc789376152.file.manager;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.SmbProperties;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class SmbFileManagerAbstract implements IFileManager {
    Logger logger = Logger.getLogger(IFileManager.class.getName());
    private String url;
    protected SmbProperties smbProperties;
    private FileProperties fileProperties;

    public abstract SmbProperties getSmbProperties();

    public void init(FileProperties fileProperties) {
        logger.info("use ftpFileManager");
        if (smbProperties == null) {
            smbProperties = getSmbProperties();
            if (smbProperties == null) {
                throw new RuntimeException("未进行smb配置");
            }
        }
        this.fileProperties = fileProperties;
        //初始化路径
        if (smbProperties.getUrl() == null || smbProperties.getUrl() == "") {
            throw new RuntimeException("未配置路径");
        }
        if (smbProperties.getUsername() != null && !smbProperties.getUsername().equals("") && smbProperties.getPassword() != null && !smbProperties.getPassword().equals("")) {
            url = "smb://" + smbProperties.getUsername() + ":" + smbProperties.getPassword() + "@" + smbProperties.getUrl();
        } else {
            url = "smb://" + smbProperties.getUrl();
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url += "/" + fileProperties.getProject() + "/" + fileProperties.getWorkDir();
        try {
            SmbFile smbFile = new SmbFile(url);
            if (!smbFile.exists()) {
                smbFile.mkdirs();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SmbException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllFilesName() {
        List<String> fileNameList = new ArrayList<String>();
        try {
            SmbFile smbFile = new SmbFile(url);
            SmbFile[] files = smbFile.listFiles();
            for (SmbFile f : files) {
                fileNameList.add(f.getName());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SmbException e) {
            e.printStackTrace();
        }
        return fileNameList;
    }

    public void upload(String s, InputStream inputStream) throws IOException {
        OutputStream outputStream = null;
        try {
            SmbFile smbFile = new SmbFile(url + "/" + s);
            smbFile.connect();
            outputStream = new SmbFileOutputStream(smbFile);
            byte[] buffer = new byte[4096];
            int len = 0; // 读取长度
            while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            // 刷新缓冲的输出流
            outputStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void download(String s, OutputStream outputStream) throws IOException {
        SmbFile smbFile;
        SmbFileInputStream smbFileInputStream = null;
        try {
            // smb://userName:passWord@host/path/shareFolderPath/fileName
            smbFile = new SmbFile(url + "/" + s);
            smbFileInputStream = new SmbFileInputStream(smbFile);
            byte[] buff = new byte[2048];
            int len;
            while ((len = smbFileInputStream.read(buff)) != -1) {
                outputStream.write(buff, 0, len);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SmbException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                smbFileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void delete(String s) throws IOException {
        SmbFile smbFile;
        try {
            // smb://userName:passWord@host/path/shareFolderPath/fileName
            smbFile = new SmbFile(url + "/" + s);
            if (smbFile.exists()) {
                smbFile.delete();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SmbException e) {
            e.printStackTrace();
        }
    }

    public void changeWorkDir(String s) throws IOException {
        if (smbProperties.getUsername() != null && !smbProperties.getUsername().equals("") && smbProperties.getPassword() != null && !smbProperties.getPassword().equals("")) {
            url = "smb://" + smbProperties.getUsername() + ":" + smbProperties.getPassword() + "@" + smbProperties.getUrl();
        } else {
            url = "smb://" + smbProperties.getUrl();
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url += "/" + fileProperties.getProject() + "/" + fileProperties.getWorkDir() + "/" + s;
    }
}
