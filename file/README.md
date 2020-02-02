# Java 文件处理组件
>[简介](#简介)  
>[使用说明](#使用说明)  
>>[IFileService接口](#IFileService接口)  
>>[IFileManager接口](#IFileManager接口)  
>
>[参数说明](#参数说明)  
>>[FileProperties](#FileProperties)  
>>[FtpProperties](#FtpProperties)  
>
>[二开说明](#二开说明)  

## 简介

为方便项目进行文件保存、下载、删除操作编写的组件。

支持上传本地服务器（通过LocalFileManager实现），以及上传至FTP文件服务器（通过FtpFileManager实现）。

开启临时文件夹功能：先将文件上传至临时文件夹，再将临时文件保存至工作文件夹。配置定时清理临时文件夹任务。

开启文件缓存功能：需开启临时文件夹功能，将需要保存的文件放到上传文件缓存中，后台定时将文件缓存上传至工作文件夹；将需要下载的文件放入下载文件缓存中，同时从工作文件夹下载文件，后台再定时将文件下载至临时文件夹。

减少用户文件保存响应时间。

## 使用说明

[项目引用](https://search.maven.org/artifact/com.github.wzc789376152/file)
目前最新可用版本：[1.0.5](https://search.maven.org/artifact/com.github.wzc789376152/file/1.0.5/jar)

SpringBoot项目[使用说明](https://github.com/wzc789376152/component/blob/master/filespringbootstarter/README.md#java-%E6%96%87%E4%BB%B6%E5%A4%84%E7%90%86%E7%BB%84%E4%BB%B6springboot)  
SpringMvc项目[使用说明](https://github.com/wzc789376152/component/blob/master/filespringmvc/README.md#java-%E6%96%87%E4%BB%B6%E5%A4%84%E7%90%86%E7%BB%84%E4%BB%B6springmvc)  

### IFileService接口
抽象类 FileServiceAbstract；协调FileManager实现文件操作；配置FileProperties以及FileManager

例如：

    IFileService fileService = new FileServiceAbstract() {
                @Override
                public FileProperties getProperties() {
                    FileProperties fileProperties = new FileProperties();
                    fileProperties.setProject("fileDemo");
                    fileProperties.setWorkDir("temp");
                    return fileProperties;
                }
    
                @Override
                public IFileManager getFileManager() {
                    IFileManager fileManager = new LocalFileManangerAbstract();
                    return fileManager;
                }
            };
        
|方法|参数|返回值|说明|
|----|----|----|----|
|`FileProperties getProperties()`|无|`FileProperties`：文件配置|返回文件配置方法|
|`IFileManager getFileManager()`|无|`IFileManager`：文件管理器|返回文件管理器方法|
|`List<String> getFileNameList() throws IOException`|无|`List<String>`：文件名集合|返回文件夹所有文件名方法|
|`void uploadCache(InputStream inputStream, String filename) throws IOException`|`inputStream`：输入文件流  `filename`：文件名|无|上传临时文件方法|
|`void submit(String filename) throws IOException`|`filename`：文件名|无|将临时文件持久化方法|
|`void download(String fileName, OutputStream outputStream) throws IOException`|`fileName`：文件名  `outputStream`：输出文件流|无|下载文件方法|
|`void delete(String filename) throws IOException`|`filename`：文件名|无|删除文件方法|

### IFileManager接口
提供两个抽象类；用于实现实际文件操作
>LocalFileManagerAbstract

    IFileManager fileManager = new LocalFileManangerAbstract();
                
>FtpFileManagerAbstract

    IFileManager fileManager = new FtpFileManagerAbstract() {
                        @Override
                        public FtpProperties ftpProperties() {
                            FtpProperties ftpProperties = new FtpProperties();
                            return ftpProperties;
                        }
                    };
    
|方法|参数|返回值|说明|
|----|----|----|----|
|`void init(FileProperties fileProperties)`|`fileProperties`：文件配置|无|初始化方法|
|`List<String> getAllFilesName()`|无|`List<String>`：文件名集合|返回文件夹所有文件名方法|
|`void upload(String filename, InputStream inputStream) throws IOException`|`filename`：文件名  `inputStream`：输入文件流|无|文件上传方法|
|`void download(String filename, OutputStream outputStream) throws IOException`|`filename`：文件名  `outputStream`：输出文件流|无|文件下载方法|
|`void delete(String filename) throws IOException`|`filename`：文件名|无|文件删除方法|

## 参数说明

### FileProperties

|  参数   | 说明  |
|  ----  | ----  |
| taskStartTime  | 临时文件清理时间：0-24小时;-1 立即开始 |
| taskPeriod  | 临时文件清理周期 |
| taskUnit  | 临时文件清理周期单位，1、year：每n年；2、month：每n月；3、day：每n天，默认为每天清理 |
| project  | 项目名，用来区分保存文件的项目文件夹 |
| workDir  | 保存文件的文件夹 |
| isCache  | 是否使用文件缓存，默认关闭，使用缓存前提是使用临时文件 |
| isTemporary  | 是否使用临时文件，默认关闭 |

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
| enable  | 是否开启ftp |

## 二开说明
