package com.github.wzc789376152.serverchan;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class PushServerChan {
    private final String sendKey;
    private final String uri;

    public PushServerChan(String sendKey) {
        this.sendKey = sendKey;
        this.uri = "https://sctapi.ftqq.com/" + sendKey + ".send";
    }

    public String sendMessage(String title, String content) {
        String result = null;
        PrintWriter out = null;
        InputStream in = null;
        String param = "title=" + title + "&desp=" + content;
        try {
            URL url = new URL(uri);
            HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
            urlcon.setDoInput(true);
            urlcon.setDoOutput(true);
            urlcon.setUseCaches(false);
            urlcon.setRequestMethod("POST");
            urlcon.connect();// 获取连接
            out = new PrintWriter(urlcon.getOutputStream());
            out.print(param);
            out.flush();
            in = urlcon.getInputStream();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder bs = new StringBuilder();
            String line = null;
            while ((line = buffer.readLine()) != null) {
                bs.append(line);
            }
            result = bs.toString();
        } catch (Exception e) {
            System.out.println("[请求异常][地址：" + uri + "][参数：" + param + "][错误信息："
                    + e.getMessage() + "]");
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
                if (null != out) {
                    out.close();
                }
            } catch (Exception e2) {
                System.out.println("[关闭流异常][错误信息：" + e2.getMessage() + "]");
            }
        }
        return result;
    }
}
