package com.github.wzc789376152.shiro.utils;

import org.springframework.web.context.request.RequestAttributes;
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
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        //cast localhost
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = getServerIp();
        }
        return ip;
    }

    public static String getIpAddr() {
        return getIpAddr(getRequest());
    }

    public static boolean checkIp(List<String> ipWhileList) {
        if(ipWhileList == null || ipWhileList.size() == 0){
            return false;
        }
        String ipStr = IpUtil.getIpAddr();
        if (ipStr != null) {
            String[] ips = ipStr.split(",");
            for (String ip : ips) {
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
            }
        }
        return false;
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

    /**
     * 获取request
     */
    public static HttpServletRequest getRequest() {
        return getRequestAttributes() == null ? null : getRequestAttributes().getRequest();
    }

    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return (ServletRequestAttributes) attributes;
    }
}
