# Java 文件处理组件(SpringBoot)

[详细说明](https://github.com/wzc789376152/component/tree/master/file)

## 使用说明
[项目引用](https://search.maven.org/artifact/com.github.wzc789376152/file-springboot-starter)
目前最新可用版本：[1.0.2](https://search.maven.org/artifact/com.github.wzc789376152/file-springboot-starter/1.0.2/jar)

## 配置说明
application.yml文件配置

### Local

    spring:
      cqfile:
        type: local
        project: salehosing
        temporary: true
        local:
          work-dir: images

### Ftp

    spring:
      cqfile:
        type: ftp
        project: salehosing
        cache: true
        temporary: true
        ftp:
          username: salehosing
          password: dBZxK2TjLZrNKWrj
          host: yangmh.top
          encoding: UTF-8
          max-wait-millis: 300
          work-dir: images
          
详细参数参考[参数说明](https://github.com/wzc789376152/component/tree/master/file#%E5%8F%82%E6%95%B0%E8%AF%B4%E6%98%8E)