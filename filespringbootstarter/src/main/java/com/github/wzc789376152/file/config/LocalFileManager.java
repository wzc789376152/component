package com.github.wzc789376152.file.config;

import com.github.wzc789376152.file.manager.IFileManager;
import com.github.wzc789376152.file.manager.LocalFileManangerAbstract;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(0)
@Component
@ConditionalOnMissingBean(IFileManager.class)
public class LocalFileManager extends LocalFileManangerAbstract {
}
