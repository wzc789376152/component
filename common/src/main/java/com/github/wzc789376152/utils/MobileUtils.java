package com.github.wzc789376152.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class MobileUtils {
    private static final String CHINA_TELECOM_PATTERN = "(?:^(?:\\+86)?1(?:33|49|53|73|77|8[019]|99)\\d{8}$)|(?:^(?:\\+86)?1349\\d{7}$)|(?:^(?:\\+86)?1410\\d{7}$)|(?:^(?:\\+86)?170[0-2]\\d{7}$)";
    private static final String CHINA_UNICOM_PATTERN = "(?:^(?:\\+86)?1(?:3[0-2]|4[56]|5[56]|66|7[156]|8[56])\\d{8}$)|(?:^(?:\\+86)?170[47-9]\\d{7}$)";
    private static final String CHINA_MOBILE_PATTERN = "(?:^(?:\\+86)?1(?:3[4-9]|4[78]|5[0-27-9]|78|8[2-478]|98)\\d{8}$)|(?:^(?:\\+86)?1440\\d{7}$)|(?:^(?:\\+86)?170[356]\\d{7}$)";
    private static final String PHONE_PATTERN = "(?:^((\\+[0-9]{2})|([0-9]{2}))?[1]([3-9])[0-9]{9}$)";

    public static boolean checkPhone(String phone) {
        if (StringUtils.isNotBlank(phone)) {
            if (checkMobile(phone)) {
                return true;
            }

            if (checkChinaMobile(phone) || checkChinaUnicom(phone) || checkChinaTelecom(phone)) {
                return true;
            }
        }

        return false;
    }

    public static boolean checkMobile(String phone) {
        if (StringUtils.isNotBlank(phone)) {
            Pattern regexp = Pattern.compile("(?:^((\\+[0-9]{2})|([0-9]{2}))?[1]([3-9])[0-9]{9}$)");
            if (regexp.matcher(phone).matches()) {
                return true;
            }
        }

        return false;
    }

    public static boolean checkChinaMobile(String phone) {
        if (StringUtils.isNotBlank(phone)) {
            Pattern regexp = Pattern.compile("(?:^(?:\\+86)?1(?:3[4-9]|4[78]|5[0-27-9]|78|8[2-478]|98)\\d{8}$)|(?:^(?:\\+86)?1440\\d{7}$)|(?:^(?:\\+86)?170[356]\\d{7}$)");
            if (regexp.matcher(phone).matches()) {
                return true;
            }
        }

        return false;
    }

    public static boolean checkChinaUnicom(String phone) {
        if (StringUtils.isNotBlank(phone)) {
            Pattern regexp = Pattern.compile("(?:^(?:\\+86)?1(?:3[0-2]|4[56]|5[56]|66|7[156]|8[56])\\d{8}$)|(?:^(?:\\+86)?170[47-9]\\d{7}$)");
            if (regexp.matcher(phone).matches()) {
                return true;
            }
        }

        return false;
    }

    public static boolean checkChinaTelecom(String phone) {
        if (StringUtils.isNotBlank(phone)) {
            Pattern regexp = Pattern.compile("(?:^(?:\\+86)?1(?:33|49|53|73|77|8[019]|99)\\d{8}$)|(?:^(?:\\+86)?1349\\d{7}$)|(?:^(?:\\+86)?1410\\d{7}$)|(?:^(?:\\+86)?170[0-2]\\d{7}$)");
            if (regexp.matcher(phone).matches()) {
                return true;
            }
        }

        return false;
    }

    public static String hideMiddleMobile(String phone) {
        if (StringUtils.isNotBlank(phone)) {
            phone = phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        }

        return phone;
    }
}
