# Java 文件处理组件(SpringBoot)

[详细说明](https://github.com/wzc789376152/component/blob/master/file/README.md#java-%E6%96%87%E4%BB%B6%E5%A4%84%E7%90%86%E7%BB%84%E4%BB%B6)

## 使用说明
SpringBoot版本>=2.0.0.RELEASE  
[项目引用](https://search.maven.org/artifact/com.github.wzc789376152/file-springboot-starter)
目前最新可用版本：[1.2.0](https://search.maven.org/artifact/com.github.wzc789376152/file-springboot-starter/1.2.0/jar)  
注意，需要同时引用包[com.github.wzc789376152.file](https://search.maven.org/artifact/com.github.wzc789376152/file)  
如果使用ftp文件管理器插件需要引入包[com.github.wzc789376152.ftp-file-manager](https://search.maven.org/artifact/com.github.wzc789376152/ftp-file-manager)
推荐使用最新版本，历史版本可能存在bug  

SpringMvc项目[使用说明](https://github.com/wzc789376152/component/blob/master/filespringmvc/README.md#java-%E6%96%87%E4%BB%B6%E5%A4%84%E7%90%86%E7%BB%84%E4%BB%B6springmvc)  

## 配置说明
application.yml文件配置

### Local

    spring:
      cqfile:
        project: fileDemo
        work-dir: images
        temporary: true

### Ftp

开启ftp文件管理器插件配置：spring.cqfile.ftp.enable=true

    spring:
      cqfile:
        project: fileDemo
        work-dir: images
        cache: true
        temporary: true
        ftp:
          enable: true
          username: 123456
          password: 123456
          host: yangmh.top
          encoding: UTF-8
          max-wait-millis: 300
          
详细参数参考[参数说明](https://github.com/wzc789376152/component/blob/master/file/README.md#%E5%8F%82%E6%95%B0%E8%AF%B4%E6%98%8E)