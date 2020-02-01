package com.github.wzc789376152.file.manager;

import com.github.wzc789376152.file.config.ftp.FtpProperties;
import com.github.wzc789376152.file.manager.ftp.FtpFileManagerAbstract;

public class FtpFileManager extends FtpFileManagerAbstract {
    @Override
    public FtpProperties ftpProperties() {
        return ftpProperties;
    }
}
