package com.github.wzc789376152.filedemospringboot;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.config.ftp.FtpProperties;
import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.manager.ftp.FtpFileManagerAbstract;
import com.github.wzc789376152.file.manager.local.LocalFileManangerAbstract;
import com.github.wzc789376152.file.service.IFileService;
import com.github.wzc789376152.file.service.impl.FileServiceAbstract;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FiledemospringbootApplication {

    public static void main(String[] args) {
        IFileService fileService = new FileServiceAbstract() {
            @Override
            public FileProperties getProperties() {
                FileProperties fileProperties = new FileProperties();
                fileProperties.setProject("fileDemo");
                fileProperties.setWorkDir("temp");
                return fileProperties;
            }

            @Override
            public IFileManager getFileManager() {
                IFileManager fileManager = new FtpFileManagerAbstract() {
                    @Override
                    public FtpProperties ftpProperties() {
                        FtpProperties ftpProperties = new FtpProperties();
                        return ftpProperties;
                    }
                };
                return fileManager;
            }
        };
        SpringApplication.run(FiledemospringbootApplication.class, args);
    }

}
