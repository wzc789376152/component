package com.github.wzc789376152.file.service.impl;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.service.IFileService;
import com.github.wzc789376152.file.task.TimerConfiguration;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 如果分布式情况下，需所有服务器缓存，故不考虑多种缓存机制如redis等，一律本机服务器缓存
 * 临时文件夹与缓存文件夹：临时文件夹用于临时存放上传文件或下载文件，以解决文件冗余以及跨服务器性能等问题；缓存文件夹解决当需要做文件缓存的同时，下载或上传同一文件会导致文件损坏问题
 *
 * @author weizhenchen
 */
public abstract class FileServiceAbstract implements IFileService {
    Logger logger = Logger.getLogger(IFileService.class.getName());


    private String getTemporaryDir() {
        return System.getProperty("user.dir") + (getProperties() != null ? (getProperties().getProject() != null ? (File.separator + getProperties().getProject() + File.separator) : File.separator) : File.separator) + "temporary" + File.separator;
    }

    private String getCacheDir() {
        return System.getProperty("user.dir") + (getProperties() != null ? (getProperties().getProject() != null ? (File.separator + getProperties().getProject() + File.separator) : File.separator) : File.separator) + "cache" + File.separator;
    }

    @Override
    public abstract FileProperties getProperties();

    @Override
    public abstract IFileManager getFileManager();

    protected IFileManager fileManager;

    protected FileProperties fileProperties;

    public void setFileProperties(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    public void setFileManager(IFileManager fileManager) {
        this.fileManager = fileManager;
    }

    /**
     * 缓存文件后缀，该文件用于重启服务器后，继续之前的缓存任务用
     */
    private final String CACHESUFFIX = ".cache";

    //缓存文件队列
    private Set<String> cacheDownloadFileSet = new HashSet<String>();

    //上传文件队列
    private Set<String> cacheUploadFileSet = new HashSet<String>();

    private boolean isTemp = true;

    private boolean isCache = true;


    public FileServiceAbstract(FileProperties properties,IFileManager manager){
        this.fileProperties = properties;
        this.fileManager = manager;
        if (fileProperties == null) {
            throw new NullPointerException("未设置参数：fileProperties");
        }
        if (fileProperties.getProject() == null || fileProperties.getProject().equals("")) {
            throw new NullPointerException("未设置参数：fileProperites.project");
        }
        if (fileProperties.getWorkDir() == null || fileProperties.getWorkDir().equals("")) {
            throw new NullPointerException("未设置参数：fileProperites.workDir");
        }
        init(fileProperties);
        if (fileManager == null) {
            fileManager = getFileManager();
        }
        fileManager.init(fileProperties);
    }

    public FileServiceAbstract() {
        if (fileProperties == null) {
            fileProperties = getProperties();
        }
        if (fileProperties == null) {
            throw new NullPointerException("未设置参数：fileProperties");
        }
        if (fileProperties.getProject() == null || fileProperties.getProject().equals("")) {
            throw new NullPointerException("未设置参数：fileProperites.project");
        }
        if (fileProperties.getWorkDir() == null || fileProperties.getWorkDir().equals("")) {
            throw new NullPointerException("未设置参数：fileProperites.workDir");
        }
        init(fileProperties);
        if (fileManager == null) {
            fileManager = getFileManager();
        }
        fileManager.init(fileProperties);
    }

    private void init(FileProperties fileProperties) {
        isTemp = fileProperties.getTemporary();
        isCache = fileProperties.getCache();
        if (isCache) {
            if (!isTemp) {
                throw new RuntimeException("文件缓存需要开启临时文件");
            }
            //创建缓存文件夹；需要服务器权限
            File cacheFile = new File(getCacheDir());
            if (!cacheFile.exists()) {
                cacheFile.mkdirs();
                cacheFile = new File(getCacheDir());
            }
            //初始化要缓存上传的文件
            FileFilter fileFilter = f -> f.getName().endsWith(CACHESUFFIX);
            Set<String> fileNames = Arrays.stream(cacheFile.listFiles(fileFilter)).map(f -> f.getName().substring(0, f.getName().indexOf(CACHESUFFIX))).collect(Collectors.toSet());
            if (fileNames.size() > 0) {
                cacheUploadFileSet = fileNames;
            }
            //创建上传任务:默认10秒
            new TimerConfiguration(-1, 10, "second") {
                @Override
                public void runable() {
                    fileUploadCache();
                }
            };
            logger.info("创建文件缓存上传任务");
            //创建下载任务:默认10秒
            new TimerConfiguration(-1, 10, "second") {
                @Override
                public void runable() {
                    fileDownloadCache();
                }
            };
            logger.info("创建文件缓存下载任务");
        }
        if (isTemp) {
            //创建临时文件夹；需要服务器权限
            File tempFile = new File(getTemporaryDir());
            if (!tempFile.exists()) {
                tempFile.mkdirs();
            }
            //创建清理临时文件任务
            new TimerConfiguration(fileProperties.getTaskStartTime(), fileProperties.getTaskPeriod(), fileProperties.getTaskUnit()) {
                @Override
                public void runable() {
                    deleteTemp();
                }
            };
            logger.info("清理临时文件任务");
        }
    }

    private boolean deleteTemp() {
        File file = new File(getTemporaryDir());
        File[] files = file.listFiles();
        for (File f : files) {
            delFile(f);
        }
        logger.info("=======清理临时文件========");
        return true;
    }

    private boolean delFile(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                delFile(f);
            }
        }
        return file.delete();
    }

    /**
     * 下载缓存文件线程任务
     * 需等待线程执行完成，才能执行下个线程，防止同时上传多个文件，造成带宽占用
     */
    private synchronized void fileDownloadCache() {
        Set<String> fileSet = new HashSet<>();
        //复制下载队列
        for (String filename : cacheDownloadFileSet) {
            fileSet.add(filename);
        }
        for (String filename : fileSet) {
            logger.info("=======开始缓存文件========");
            try {
                //将文件缓存到缓存文件夹
                OutputStream outputStream = new FileOutputStream(new File(getCacheDir() + filename));
                try {
                    fileManager.download(filename, outputStream);
                } catch (IOException e) {
                    throw new IOException(e.getMessage());
                } finally {
                    outputStream.close();
                }
                //将缓存文件夹文件保存到临时文件夹
                File cacheFile = new File(getCacheDir() + filename);
                FileInputStream inputStream = new FileInputStream(cacheFile);
                File dest = new File(getTemporaryDir() + filename);
                FileChannel inputChannel = null;
                FileChannel outputChannel = null;
                try {
                    inputChannel = inputStream.getChannel();
                    outputChannel = new FileOutputStream(dest).getChannel();
                    outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                } catch (IOException e) {
                    throw new IOException(e.getMessage());
                } finally {
                    inputChannel.close();
                    outputChannel.close();
                    inputStream.close();
                    //删除缓存文件
                    cacheFile.delete();
                }
                cacheDownloadFileSet.remove(filename);
                logger.info("=======缓存文件成功========");
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }
    }

    /**
     * 上传缓存文件线程任务
     * 需等待线程执行完成，才能执行下个线程，防止同时上传多个文件，造成带宽占用
     */
    private synchronized void fileUploadCache() {
        Set<String> fileSet = new HashSet<>();
        //复制上传队列
        for (String filename : cacheUploadFileSet) {
            fileSet.add(filename);
        }
        for (String filename : fileSet) {
            logger.info("=======开始上传缓存文件========");
            try {
                File tempFile = new File(getTemporaryDir() + filename);
                File cacheFile = new File(getCacheDir() + filename + CACHESUFFIX);
                if (!tempFile.exists()) {
                    cacheUploadFileSet.remove(filename);
                    if (cacheFile.exists()) {
                        cacheFile.delete();
                    }
                    throw new IOException("文件不存在");
                }
                FileInputStream inputStream = new FileInputStream(tempFile);
                File dest = new File(getCacheDir() + filename);
                FileChannel inputChannel = null;
                FileChannel outputChannel = null;
                try {
                    inputChannel = inputStream.getChannel();
                    outputChannel = new FileOutputStream(dest).getChannel();
                    outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                } finally {
                    inputChannel.close();
                    outputChannel.close();
                    inputStream.close();
                }
                InputStream cacheInputStream = new FileInputStream(new File(getCacheDir() + filename));
                try {
                    fileManager.upload(filename, cacheInputStream);
                } catch (IOException e) {
                    throw new IOException(e.getMessage());
                } finally {
                    cacheInputStream.close();
                }
                dest = new File(getCacheDir() + filename);
                dest.delete();
                cacheFile = new File(getCacheDir() + filename + CACHESUFFIX);
                cacheFile.delete();
                cacheUploadFileSet.remove(filename);
                logger.info("=======上传缓存文件成功========");
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }
    }

    @Override
    public List<String> getFileNameList() {
        List<String> fileList = fileManager.getAllFilesName();
        return fileList;
    }


    @Override
    public void uploadCache(InputStream inputStream, String filename) throws IOException {
        if (isTemp) {
            File dest = new File(getTemporaryDir() + filename);
            FileChannel inputChannel = null;
            FileChannel outputChannel = null;
            try {
                inputChannel = ((FileInputStream) inputStream).getChannel();
                outputChannel = new FileOutputStream(dest).getChannel();
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            } finally {
                inputChannel.close();
                outputChannel.close();
                inputStream.close();
            }
            logger.info("保存临时文件成功！");
        } else {
            fileManager.upload(filename, inputStream);
        }
    }


    @Override
    public void submit(String filename) throws IOException {
        if (isCache) {
            cacheUploadFileSet.add(filename);
            File cacheFile = new File(getCacheDir() + filename + CACHESUFFIX);
            OutputStream outputStream = new FileOutputStream(cacheFile);
            outputStream.write(0);//创建缓存文件
            outputStream.close();
            logger.info("加入文件上传缓存队列！");
        } else {
            if (isTemp) {
                File tempFile = new File(getTemporaryDir() + filename);
                if (tempFile.exists()) {
                    InputStream inputStream = new FileInputStream(tempFile);
                    try {
                        fileManager.upload(filename, inputStream);
                    } finally {
                        inputStream.close();
                    }
                }
            } else {
                logger.warning("未开启模版，方法无效！");
            }
        }
    }

    @Override
    public void download(String fileName, OutputStream outputStream) throws IOException {
        if (isTemp) {
            File tempFile = new File(getTemporaryDir() + fileName);
            if (!tempFile.exists()) {
                //直接下载
                fileManager.download(fileName, outputStream);
                if (isCache) {
                    cacheDownloadFileSet.add(fileName);
                    logger.info("加入文件下载缓存队列！");
                }
            } else {
                InputStream tempInputStream = new FileInputStream(tempFile);
                BufferedInputStream br = new BufferedInputStream(tempInputStream);
                byte[] buf = new byte[1024];
                int len = 0;
                while ((len = br.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
                br.close();
                outputStream.close();
                tempInputStream.close();
            }
        } else {
            //直接下载
            fileManager.download(fileName, outputStream);
        }
    }

    /**
     * 删除文件
     */
    @Override
    public void delete(String filename) throws IOException {
        if (isTemp) {
            File tempFile = new File(getTemporaryDir() + filename);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        fileManager.delete(filename);
    }
}
