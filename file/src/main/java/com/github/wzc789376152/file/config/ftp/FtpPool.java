package com.github.wzc789376152.file.config.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * fileName:ftpPool
 * description:FTP连接池
 * 1.可以获取池中空闲链接
 * 2.可以将链接归还到池中
 * 3.当池中空闲链接不足时，可以创建链接
 */
public class FtpPool {
    private FtpClientFactory factory;
    private GenericObjectPool<FTPClient> internalPool;

    public FtpClientFactory getFactory() {
        return factory;
    }

    /**
     * 初始化连接池
     */
    public FtpPool(FtpClientFactory factory) {
        this.factory = factory;
        initPool();
    }

    private void initPool() {
        FtpProperties properties = factory.getProperties();
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(properties.getMaxTotal());
        poolConfig.setMinIdle(properties.getMinIdel());
        poolConfig.setMaxIdle(properties.getMaxIdle());
        poolConfig.setMaxWaitMillis(properties.getMaxWaitMillis());
        this.internalPool = new GenericObjectPool<FTPClient>(factory, poolConfig);
    }

    /**
     * 从连接池中取连接
     */
    public FTPClient getFTPClient() {
        try {
            return internalPool.borrowObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将链接归还到连接池
     */
    public void returnFTPClient(FTPClient ftpClient) {
        try {
            internalPool.returnObject(ftpClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 销毁池子
     */
    public void destroy() {
        try {
            internalPool.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
