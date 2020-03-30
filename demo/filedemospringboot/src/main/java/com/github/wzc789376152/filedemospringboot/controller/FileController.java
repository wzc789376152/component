package com.github.wzc789376152.filedemospringboot.controller;

import com.github.wzc789376152.file.service.IFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("file")
public class FileController {
    @Autowired
    private IFileService fileService;

    @PostMapping("upload")
    public boolean upload(HttpServletRequest request) throws IOException {
//将当前上下文初始化给  CommonsMutipartResolver （多部分解析器）
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
                request.getSession().getServletContext());
        //检查form中是否有enctype="multipart/form-data"
        List<String> fileList = new ArrayList<>();
        if (multipartResolver.isMultipart(request)) {
            //将request变成多部分request
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
            //获取multiRequest 中所有的文件名
            Iterator iter = multiRequest.getFileNames();
            while (iter.hasNext()) {
                //一次遍历所有文件
                MultipartFile file = multiRequest.getFile(iter.next().toString());
                String filename = UUID.randomUUID().toString();
                if (file != null) {
                    fileService.uploadCache(file.getInputStream(), filename);
                    fileService.submit(filename);
                    fileList.add(filename);
                }
            }
        }
        return true;
    }

    @PostMapping("upload1")
    public boolean upload1(HttpServletRequest request) throws IOException {
//将当前上下文初始化给  CommonsMutipartResolver （多部分解析器）
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
                request.getSession().getServletContext());
        String token = request.getParameter("filetoken");
        Long position = Long.valueOf(request.getParameter("position"));
        //检查form中是否有enctype="multipart/form-data"
        List<String> fileList = new ArrayList<>();
        if (multipartResolver.isMultipart(request)) {
            //将request变成多部分request
            MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
            //获取multiRequest 中所有的文件名
            Iterator iter = multiRequest.getFileNames();
            while (iter.hasNext()) {
                //一次遍历所有文件
                MultipartFile file = multiRequest.getFile(iter.next().toString());
                if (file != null) {
                    fileService.uploadCache(file.getInputStream(), file.getOriginalFilename(), token, position);
                    fileList.add(file.getOriginalFilename());
                }
            }
        }
        return true;
    }

    @PostMapping("submit")
    public boolean submit(String filename, String token) throws IOException {
        fileService.submit(filename, token);
        return true;
    }

    @GetMapping("getPosition")
    public Long getPosition(String filename, String filetoken) throws IOException {
        return fileService.getFilePosition(filename, filetoken);
    }
}
