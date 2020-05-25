package com.github.wzc789376152.shiro.utils;

import org.apache.shiro.codec.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

public class CipherKeyUtils {
    public static String createCipherKey() {
        try {
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            SecretKey deskey = keygen.generateKey();
            return Base64.encodeToString(deskey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
