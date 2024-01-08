package com.github.wzc789376152.utils;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpUtil {

    private static String serverIp;

    static {
        InetAddress ia = null;
        try {
            ia = InetAddress.getLocalHost();
            serverIp = ia.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取ip地址
     *
     * @param request HttpServletRequest
     * @return string
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String remoteAddr = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        String realIp = request.getHeader("X-Real-IP");

        String ip = null;
        if (realIp == null) {
            if (forwarded == null || forwarded.equals("0:0:0:0:0:0:0:1")) {
                ip = formatIpv4(remoteAddr);
            } else {
                ip = formatIpv4(forwarded);
            }
        } else {
            if (forwarded == null || forwarded.equals("0:0:0:0:0:0:0:1")) {
                ip = formatIpv4(realIp);
            } else {
                if (realIp.equals(forwarded)) {
                    ip = formatIpv4(realIp);
                } else {
                    ip = formatIpv4(forwarded);
                }
            }
        }
        return ip;
    }

    public static String getIpAddr() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        return getIpAddr(requestAttributes == null ? null : requestAttributes.getRequest());
    }


    /**
     * 获取服务器的ip地址
     *
     * @return String
     */
    public static String getServerIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                while (nias.hasMoreElements()) {
                    InetAddress ia = nias.nextElement();
                    if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress() && ia instanceof Inet4Address) {
                        serverIp = ia.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return serverIp;
    }

    /**
     * 获取所有的网卡的ip v4地址,key为网卡地址，value为ip地址
     *
     * @return hash map
     */
    public static Map<String, String> getLocalIPV4() {
        Map<String, String> map = new HashMap<>();
        InetAddress ip = null;
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    ip = ips.nextElement();
                    if (ip instanceof Inet4Address) {
                        map.put(ni.getName(), ip.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 获取所有ipv6地址
     *
     * @return hash map
     */
    public static Map<String, String> getLocalIPV6() {
        Map<String, String> map = new HashMap<>();
        InetAddress ip = null;
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    ip = ips.nextElement();
                    if (ip instanceof Inet6Address) {
                        map.put(ni.getName(), ip.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static boolean checkIp(List<String> ipWhileList) {
        if (ipWhileList == null || ipWhileList.size() == 0) {
            return false;
        }
        String ip = IpUtil.getIpAddr();
        if (StringUtils.isEmpty(ip)) {
            return false;
        }
        ip = ip.trim();
        String patternString = "(([0,1]?\\d?\\d|2[0-4]\\d|25[0-5])\\.){3}([0,1]?\\d?\\d|2[0-4]\\d|25[0-5])";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(ip);
        if (matcher.matches()) {
            String[] m = ip.split("\\.");
            String ip1 = m[0] + "." + m[1] + "." + m[2] + ".0";
            String ip2 = m[0] + "." + m[1] + ".0.0";
            String ip3 = m[0] + ".0.0.0";
            List<String> ipList = new ArrayList<>();
            ipList.add(ip);
            ipList.add(ip1);
            ipList.add(ip2);
            ipList.add(ip3);
            for (String ipWhile : ipWhileList) {
                if (ipList.contains(ipWhile)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 格式化ip为标准ipv4
     *
     * @param ip
     * @return
     */
    private static String formatIpv4(String ip) {
        ip = ip.split(",")[0];
        if ("localhost".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
