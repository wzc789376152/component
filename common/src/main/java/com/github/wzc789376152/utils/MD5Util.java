package com.github.wzc789376152.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MD5Util {


    public static byte[] encryptMD5(String data) throws IOException {
        return encryptMD5(data.getBytes(StandardCharsets.UTF_8));
    }

    //对字符串进行MD5编码加盐操作
    public static String getMD5CodeBySalt(String str, String salt) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        return getMD5Code(getMD5Code(str) + salt);
    }

    //对字符串进行MD5编码
    public static String getMD5Code(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        try {
            //创建具有指定算法名称的信息摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            //使用指定的字节数组对摘要进行最后的更新，然后完成摘要计算
            byte[] results = md.digest(str.getBytes());
            //将得到的字节数组编程字符串返回
            String resultString = byteArrayToHexString(results);
            return resultString.toUpperCase();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    //转换字节数组为十六进制字符串
    private static String byteArrayToHexString(byte[] results) {
        StringBuffer resultNew = new StringBuffer();
        for (int i = 0; i < results.length; i++) {
            resultNew.append(byteToHexString(results[i]));
        }
        return resultNew.toString();
    }

    //将字节转化成十六进制的字符串
    private static String byteToHexString(byte b) {
        int byteNum = b;
        if (byteNum < 0) {
            byteNum = byteNum + 256;
        }
        int d1 = byteNum / 16;
        int d2 = byteNum % 16;
        return HEXDIGITS[d1] + HEXDIGITS[d2];
    }

    //16进制下数字到字符的映射数组
    private static final String[] HEXDIGITS = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};


    public static byte[] encryptMD5(byte[] data) throws IOException {
        byte[] bytes = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            bytes = md.digest(data);
        } catch (GeneralSecurityException gse) {
            throw new IOException(gse.toString());
        }
        return bytes;
    }
    public static String makeMD5GBK(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data.getBytes("GBK"));    //问题主要出在这里，Java的字符串是unicode编码，不受源码文件的编码影响；而PHP的编码是和源码文件的编码一致，受源码编码影响。
            StringBuilder buf = new StringBuilder();
            for (byte b : md.digest()) {
                buf.append(String.format("%02x", b & 0xff));
            }
            return buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static String makeMD5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data.getBytes(StandardCharsets.UTF_8));    //问题主要出在这里，Java的字符串是unicode编码，不受源码文件的编码影响；而PHP的编码是和源码文件的编码一致，受源码编码影响。
            StringBuilder buf = new StringBuilder();
            for (byte b : md.digest()) {
                buf.append(String.format("%02x", b & 0xff));
            }
            return buf.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
