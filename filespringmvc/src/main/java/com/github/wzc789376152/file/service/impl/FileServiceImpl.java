package com.github.wzc789376152.file.service.impl;

import com.github.wzc789376152.file.FileProperties;
import com.github.wzc789376152.file.manager.IFileManager;

public class FileServiceImpl extends FileServiceAbstract {
    public FileServiceImpl(FileProperties properties, IFileManager manager) {
        super(properties, manager);
    }

    @Override
    public FileProperties getProperties() {
        return fileProperties;
    }

    @Override
    public IFileManager getFileManager() {
        return fileManager;
    }
}
