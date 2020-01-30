package com.github.wzc789376152.file.utils;

import java.io.File;

public class FilePathUtils {
    public static String formatPath(String path) {
        boolean isStart = path.startsWith("/") || path.startsWith("\\");
        boolean isEnd = path.endsWith("/") || path.endsWith("\\");
        if (isStart) {
            path = path.substring(1);
        }
        if (isEnd) {
            path = path.substring(0, path.length() - 1);
        }
        path = path.replace("/", File.separator).replace("\\", File.separator);
        return File.separator + path + File.separator;
    }
}
