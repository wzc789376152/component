package com.github.wzc789376152.file.service.impl;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.service.IFileService;
import com.github.wzc789376152.file.task.TimerConfiguration;
import com.github.wzc789376152.file.utils.FileSyncUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * 如果分布式情况下，需所有服务器缓存，故不考虑多种缓存机制如redis等，一律本机服务器缓存
 * 临时文件夹与缓存文件夹：临时文件夹用于临时存放上传文件或下载文件，以解决文件冗余以及跨服务器性能等问题；缓存文件夹解决当需要做文件缓存的同时，下载或上传同一文件会导致文件损坏问题
 *
 * @author weizhenchen
 */
public abstract class FileServiceAbstract implements IFileService {
    Logger logger = Logger.getLogger(IFileService.class.getName());


    public String getTemporaryDir() {
        return System.getProperty("user.dir") + (getProperties() != null ? (getProperties().getProject() != null ? (File.separator + getProperties().getProject() + File.separator) : File.separator) : File.separator) + "temporary" + File.separator;
    }

    public String getCacheDir() {
        return System.getProperty("user.dir") + (getProperties() != null ? (getProperties().getProject() != null ? (File.separator + getProperties().getProject() + File.separator) : File.separator) : File.separator) + "cache" + File.separator;
    }

    public abstract FileProperties getProperties();

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

    private final String BLOCKSUFFIX = ".block";

    //缓存文件队列
//    private Queue<String> cacheDownloadFileQueue = new LinkedBlockingQueue<String>();

    //上传文件队列
//    private Queue<String> cacheUploadFileQueue = new LinkedBlockingQueue<String>();

    //下载文件线程队列
    private ExecutorService cacheDownloadExecutor = Executors.newCachedThreadPool();
    //上传文件队列
    private ExecutorService cacheUploadExecutor = Executors.newCachedThreadPool();

    private boolean isTemp = true;

    private boolean isCache = true;


    public FileServiceAbstract(FileProperties properties, IFileManager manager) {
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
            //初始化要缓存上传的文件
            final FileFilter fileFilter = new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(CACHESUFFIX);
                }
            };
            //创建缓存队列
            final File finalCacheFile = new File(getCacheDir());
            if (finalCacheFile.exists()) {
                for (File file : finalCacheFile.listFiles(fileFilter)) {
                    final String filename = file.getName().substring(0, file.getName().indexOf(CACHESUFFIX));
                    Runnable runnable = new Runnable() {
                        public void run() {
                            fileUploadCache(filename);
                        }
                    };
                    cacheUploadExecutor.execute(runnable);
                }
            }
        }
        if (isTemp) {
            //创建清理临时文件任务
            new TimerConfiguration(fileProperties.getTaskStartTime(), fileProperties.getTaskPeriod(), fileProperties.getTaskUnit()) {
                @Override
                public boolean runable() {
                    logger.info("=======开始清理临时文件========");
                    deleteTemp();
                    logger.info("=======清理临时文件成功========");
                    return true;

                }
            };
            logger.info("创建清理临时文件任务");
        }
    }

    private void mkdirs(String filepath) throws IOException {
        try {
            File file = new File(filepath);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            throw new IOException("创建文件夹：" + filepath + "失败，请检查系统权限");
        }
    }

    private void deleteTemp() {
        File file = new File(getTemporaryDir());
        if (file.exists()) {
            File[] files = file.listFiles();
            for (File f : files) {
                delFile(f);
            }
        }
    }

    private void delFile(File file) {
        if (!file.exists()) {
            logger.warning(file.getName() + "不存在");
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                delFile(f);
            }
        }
        file.delete();
        logger.info(file.getName() + "删除成功");
    }

    private void delFiles(File[] files) {
        for (File file : files) {
            delFile(file);
        }
    }

    /**
     * 下载缓存文件线程任务
     * 需等待线程执行完成，才能执行下个线程，防止同时上传多个文件，造成带宽占用
     */
    private synchronized void fileDownloadCache(String filename) {
        logger.info("=======开始缓存文件:" + filename + "========");
        try {
            mkdirs(getCacheDir());
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
            mkdirs(getTemporaryDir());
            synchronized (FileSyncUtils.getObject(getTemporaryDir() + filename)) {
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
//            cacheDownloadFileQueue.remove(filename);
                logger.info("=======缓存文件:" + filename + "成功========");
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }

    /**
     * 上传缓存文件线程任务
     * 需等待线程执行完成，才能执行下个线程，防止同时上传多个文件，造成带宽占用
     */
    private synchronized void fileUploadCache(String filename) {
        logger.info("=======开始上传缓存文件:" + filename + "========");
        try {
            mkdirs(getTemporaryDir());
            File tempFile = new File(getTemporaryDir() + filename);
            File cacheFile = new File(getCacheDir() + filename + CACHESUFFIX);
            if (!tempFile.exists()) {
//                cacheUploadFileQueue.remove(filename);
                if (cacheFile.exists()) {
                    cacheFile.delete();
                }
                throw new IOException("文件不存在");
            }
            FileInputStream inputStream = new FileInputStream(tempFile);
            try {
                fileManager.upload(filename, inputStream);
            } catch (IOException e) {
                throw new IOException(e.getMessage());
            } finally {
                inputStream.close();
            }
//            dest = new File(getCacheDir() + filename);
//            dest.delete();
            cacheFile = new File(getCacheDir() + filename + CACHESUFFIX);
            cacheFile.delete();
//            cacheUploadFileQueue.remove(filename);
            logger.info("=======上传缓存文件:" + filename + "成功========");
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
    }

    public List<String> getFileNameList() {
        List<String> fileList = fileManager.getAllFilesName();
        return fileList;
    }

    public Long getFilePosition(final String filename, final String token) {
        File tempFile = new File(getTemporaryDir());
        if (!tempFile.exists()) {
            return Long.valueOf(0);
        }
        final FileFilter fileFilter = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(filename + "__" + token);
            }
        };
        File[] files = tempFile.listFiles(fileFilter);
        if (files.length == 0) {
            return Long.valueOf(0);
        }
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                long diff = o1.lastModified() - o2.lastModified();
                if (diff < 0) {
                    return 1;//倒序正序控制
                } else if (diff == 0) {
                    return 0;
                } else {
                    return -1;//倒序正序控制
                }
            }
        });
        String[] filenames = files[0].getName().split("__");
        return Long.valueOf(filenames[2]);
    }

    public void uploadCache(InputStream inputStream, String filename) throws IOException {
        if (isTemp) {
            mkdirs(getTemporaryDir());
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

    public void uploadCache(InputStream inputStream, String filename, String token, Long position) throws IOException {
        if (token != null && position != null) {
            filename = filename + "__" + token + "__" + position;
        }
        uploadCache(inputStream, filename);
    }

    public void submit(final String filename) throws IOException {
        if (isCache) {
            mkdirs(getCacheDir());
            File cacheFile = new File(getCacheDir() + filename + CACHESUFFIX);
            OutputStream outputStream = new FileOutputStream(cacheFile);
            outputStream.write(0);//创建缓存文件
            outputStream.close();
            cacheUploadExecutor.execute(new Runnable() {
                public void run() {
                    fileUploadCache(filename);
                }
            });
            logger.info(filename + "加入文件上传缓存队列！");
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
                logger.warning("未开启临时文件，方法无效！");
            }
        }
    }

    public void submit(String filename, String token) throws IOException {
        final String fileName = filename;
        final String toKen = token;
        File tempFile = new File(getTemporaryDir());
        final FileFilter fileFilter = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(fileName + "__" + toKen);
            }
        };
        File[] files = tempFile.listFiles(fileFilter);
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                long diff = o1.lastModified() - o2.lastModified();
                if (diff > 0) {
                    return 1;//倒序正序控制
                } else if (diff == 0) {
                    return 0;
                } else {
                    return -1;//倒序正序控制
                }
            }
        });
        File dest = new File(getTemporaryDir() + filename);
        FileChannel outChannel = null;
        int byteSize = 1024 * 8;
        try {
            outChannel = new FileOutputStream(dest).getChannel();
            for (File f : files) {
                FileChannel fc = new FileInputStream(f).getChannel();
                ByteBuffer bb = ByteBuffer.allocate(byteSize);
                while (fc.read(bb) != -1) {
                    bb.flip();
                    outChannel.write(bb);
                    bb.clear();
                }
                fc.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (outChannel != null) {
                    outChannel.close();
                }
            } catch (IOException ignore) {
            }
        }
        submit(filename);
        delFiles(files);
    }

    public void download(final String fileName, OutputStream outputStream) throws IOException {
        if (isTemp) {
            File tempFile = new File(getTemporaryDir() + fileName);
            if (!tempFile.exists() || tempFile.length() == 0) {
                //直接下载
                synchronized (FileSyncUtils.getObject(getTemporaryDir() + fileName)) {
                    if (outputStream != null) {
                        fileManager.download(fileName, outputStream);
                    }
                    if (isCache) {
                        cacheDownloadExecutor.execute(new Runnable() {
                            public void run() {
                                fileDownloadCache(fileName);
                            }
                        });
                        logger.info(fileName + "加入文件下载缓存队列！");
                    }
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
