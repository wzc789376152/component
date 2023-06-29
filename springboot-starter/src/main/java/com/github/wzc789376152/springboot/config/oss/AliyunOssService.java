package com.github.wzc789376152.springboot.config.oss;


import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.github.wzc789376152.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;


public class AliyunOssService {
    private final OSS ossClient;
    private final AliyunOssConfig aliyunOssConfig;

    public AliyunOssService(OSS ossClient, AliyunOssConfig aliyunOssConfig) {
        this.ossClient = ossClient;
        this.aliyunOssConfig = aliyunOssConfig;
    }

    public String upload(InputStream inputStream, String fileName) {
        return upload(inputStream, fileName, null);
    }

    public String upload(InputStream inputStream, String fileName, String contentType) {
        String path = getPath(fileName);
        PutObjectRequest putObjectRequest = new PutObjectRequest(aliyunOssConfig.getBucketName(), path, inputStream);
        if (StringUtils.isNotEmpty(contentType)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            putObjectRequest.setMetadata(metadata);
        }
        ossClient.putObject(putObjectRequest);
        return aliyunOssConfig.getDomain() + "/" + path;
    }

    public String upload(byte[] data, String fileName) {
        return upload(new ByteArrayInputStream(data), fileName, null);
    }

    public String upload(byte[] data, String fileName, String contentType) {
        return upload(new ByteArrayInputStream(data), fileName, contentType);
    }

    public Boolean delete(String path) {
        path = path.replaceAll(aliyunOssConfig.getDomain() + "/", "");
        ossClient.deleteObject(aliyunOssConfig.getBucketName(), path);
        return true;
    }

    private String getPath(String fileName) {
        //获取最后一个.的位置
        int lastIndexOf = fileName.lastIndexOf(".");
        //获取文件的后缀名 .jpg
        String suffix = "";
        if (lastIndexOf >= 0) {
            suffix = fileName.substring(lastIndexOf);
        }
        //生成uuid
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        //文件路径
        String path = DateUtils.format(DateUtils.now(), "yyyyMMdd") + "/" + uuid;
        if (StringUtils.isNotEmpty(aliyunOssConfig.getPrefix())) {
            path = aliyunOssConfig.getPrefix() + "/" + path;
        }
        return path + suffix;
    }
}
