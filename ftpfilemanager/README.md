# Java 文件处理组件ftp拓展插件

[详细说明](https://github.com/wzc789376152/component/blob/master/file/README.md#java-%E6%96%87%E4%BB%B6%E5%A4%84%E7%90%86%E7%BB%84%E4%BB%B6)

## 使用说明
[项目引用](https://search.maven.org/artifact/com.github.wzc789376152/ftpfilemanager)
目前最新可用版本：[1.2.0](https://search.maven.org/artifact/com.github.wzc789376152/ftpfilemanager/1.2.0/jar)  
注意，需要同时引用[项目](https://search.maven.org/artifact/com.github.wzc789376152/file)  
推荐使用最新版本，历史版本可能存在bug  

SpringBoot项目[使用说明](https://github.com/wzc789376152/component/blob/master/filespringbootstarter/README.md#java-%E6%96%87%E4%BB%B6%E5%A4%84%E7%90%86%E7%BB%84%E4%BB%B6springboot)  
SpringMvc项目[使用说明](https://github.com/wzc789376152/component/blob/master/filespringmvc/README.md#java-%E6%96%87%E4%BB%B6%E5%A4%84%E7%90%86%E7%BB%84%E4%BB%B6springmvc)  

## 参数说明

### FtpProperties

|  参数   | 说明  |
|  ----  | ----  |
| host  | ftp服务器路径 |
| port  | ftp服务器端口 |
| username  | ftp用户名 |
| password  | ftp密码 |
| encoding  | 编码格式 |
| maxTotal  | 最大连接数 |
| minIdel  | 最小空闲连接 |
| maxIdle  | 最大空闲连接 |
| maxWaitMillis  | 请求连接最大等待时间(毫秒) |
          
详细参数参考[参数说明](https://github.com/wzc789376152/component/blob/master/file/README.md#%E5%8F%82%E6%95%B0%E8%AF%B4%E6%98%8E)