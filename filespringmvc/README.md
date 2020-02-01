# Java 文件处理组件(SpringMvc)

[详细说明](https://github.com/wzc789376152/component/blob/master/file/README.md#java-%E6%96%87%E4%BB%B6%E5%A4%84%E7%90%86%E7%BB%84%E4%BB%B6)

## 使用说明
[项目引用](https://search.maven.org/artifact/com.github.wzc789376152/filespringmvc)
目前最新可用版本：[1.0.4](https://search.maven.org/artifact/com.github.wzc789376152/filespringmvc/1.0.4/jar)

SpringBoot项目[使用说明](https://github.com/wzc789376152/component/blob/master/filespringbootstarter/README.md#java-%E6%96%87%E4%BB%B6%E5%A4%84%E7%90%86%E7%BB%84%E4%BB%B6springboot)  

## 配置说明

### web.xml文件配置

    <init-param>
      <param-name>contextConfigLocation</param-name>
      <param-value>classpath:spring.xml,classpath*:spring-file-local.xml</param-value>
    </init-param>

添加spring配置文件，如本地配置添加classpath*:spring-file-local.xml
ftp配置添加classpath*:spring-file-ftp.xml

### file.properties文件配置
cqfile.project=fileDemo  
cqfile.workDir=temp  

cqfile.ftp.host=127.0.0.1  
cqfile.ftp.username=123456  
          
详细参数参考[参数说明](https://github.com/wzc789376152/component/blob/master/file/README.md#%E5%8F%82%E6%95%B0%E8%AF%B4%E6%98%8E)