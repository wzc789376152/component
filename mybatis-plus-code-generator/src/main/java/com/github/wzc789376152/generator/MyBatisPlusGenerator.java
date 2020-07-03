package com.github.wzc789376152.generator;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.github.wzc789376152.generator.common.service.IBaseService;
import com.github.wzc789376152.generator.common.service.impl.BaseServiceImpl;
import com.github.wzc789376152.generator.config.JdbcPropertis;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class MyBatisPlusGenerator {
    private JdbcPropertis jdbcPropertis;
    //需要生成的表，数组
    private String[] youTableName = {};
    //【实体】是否为lombok模型（默认 false）
    private boolean lombok_flag = false;

    //作者
    private String author = "yangmenghen";
    private boolean override = false;
    private String packageName = "";
    private String layoutUrl = "";


    //以下参数已更换为动态获取
    //项目的本地路径
    //private static String demo_path="F:\\IDEAWorkspaces\\eapp-door-control-api";
    //包名
    //private static String packge_name="com.lg.eappdoorcontrolapi";


    public void run(JdbcPropertis jdbcPropertis, String packageName, String layoutUrl) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("是否生成所有表？(y/n)" );
        String isAll = scanner.next();
        if (!isAll.toLowerCase().equals("y" )) {
            System.out.print("请输入要生成的表：" );
            String tables = scanner.next();
            if (tables != null && !tables.equals("" )) {
                this.youTableName = tables.split("," );
            }
        }
        System.out.print("是否覆盖？(y/n)" );
        String overrideStr = scanner.next();
        overrideStr = overrideStr.toLowerCase();
        if (overrideStr.equals("y" )) {
            override = true;
        } else {
            override = false;
        }
        this.jdbcPropertis = jdbcPropertis;
        this.packageName = packageName;
        this.layoutUrl = layoutUrl;
        getCode_model();
    }

    /**
     * 自动生成代码
     */
    private void getCode_model() throws IOException {

        AutoGenerator mpg = new AutoGenerator();

        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        File directory = new File("" );// 参数为空
        final String demo_path = directory.getCanonicalPath();

        gc.setOutputDir(demo_path + File.separator + "src" + File.separator + "main" + File.separator + "java" );// 生成路径
        gc.setFileOverride(override);// 是否覆盖
        gc.setActiveRecord(false);//是否支持AR模式
        gc.setEnableCache(false);// XML 二级缓存
        gc.setBaseResultMap(true);// XML ResultMap
        gc.setBaseColumnList(false);// XML columList
        gc.setAuthor(author);//作者
        //gc.setIdType(IdType.UUID); // 主键策略

        // 自定义文件命名，注意 %s 会自动填充表实体属性！
        gc.setMapperName("%sDao" );
        gc.setXmlName("%sMapper" );
        gc.setServiceName("%sService" );
        gc.setServiceImplName("%sServiceImpl" );
        gc.setControllerName("%sController" );
        mpg.setGlobalConfig(gc);

        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setDbType(DbType.getDbType(jdbcPropertis.getType()));
        dsc.setTypeConvert(new MySqlTypeConvert() {

//            public DbColumnType processTypeConvert(GlobalConfig globalConfig, String fieldType){
//                //将数据库中datetime转换成date
//                if ( fieldType.toLowerCase().contains( "datetime" ) ) {
//                    return DbColumnType.DATE;
//                }
//                return (DbColumnType) super.processTypeConvert(globalConfig, fieldType);
//            }

        });
//        dsc.setTypeConvert(new OracleTypeConvert() {
//            // 自定义数据库表字段类型转换
//            @Override
//            public DbColumnType processTypeConvert(String fieldType) {
//                return (DbColumnType) super.processTypeConvert(fieldType);
//            }
//        });


        //配置数据源
        dsc.setDriverName(jdbcPropertis.getDriverName());
        dsc.setUsername(jdbcPropertis.getUsername());
        dsc.setPassword(jdbcPropertis.getPassword());
        dsc.setUrl(jdbcPropertis.getUrl());
        mpg.setDataSource(dsc);
        // 策略配置
        StrategyConfig strategy = new StrategyConfig();
        //strategy.setTablePrefix(new String[]{"D_"});// 此处修改为表前缀
        strategy.setNaming(NamingStrategy.underline_to_camel);// 表名生成策略
        if (youTableName.length > 0) {
            strategy.setInclude(youTableName); // 需要生成的表
        } else {
            strategy.setExclude(new String[]{"flyway_schema_history"});
        }
        strategy.setEntityTableFieldAnnotationEnable(true);
        strategy.setSuperServiceClass(IBaseService.class);
        strategy.setSuperServiceImplClass(BaseServiceImpl.class);
        mpg.setStrategy(strategy);
        // 包配置
        PackageConfig pc = new PackageConfig();
        pc.setParent(packageName);
        pc.setEntity("entity" );
        pc.setMapper("dao" );
        pc.setXml("mapper" );
        pc.setService("service" );
        pc.setServiceImpl("service.impl" );
        pc.setController("controller" );
        mpg.setPackageInfo(pc);
        // 注入自定义配置，可以在 VM 中使用 cfg.abc 设置的值
        final String finalPackageName = packageName;
        InjectionConfig cfg = new InjectionConfig() {
            @Override
            public void initMap() {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("abc", this.getConfig().getGlobalConfig().getAuthor() + "-mp" );
                map.put("model", finalPackageName + ".model" );
                map.put("layoutUrl", layoutUrl);
                this.setMap(map);
            }
        };
        List<FileOutConfig> foc = new ArrayList<FileOutConfig>();
        final String finalPath = demo_path + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + packageName.replace(".", File.separator) + File.separator + "model" + File.separator;
        // 调整 xml 生成目录演示
        foc.add(new FileOutConfig(File.separator + "templates" + File.separator + "mymapper.xml.vm" ) {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return demo_path + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "mapper" + File.separator + tableInfo.getEntityName() + "Mapper.xml";
            }
        });
        //调整控制器
        foc.add(new FileOutConfig(File.separator + "templates" + File.separator + "mycontroller.java.vm" ) {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return demo_path + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + packageName.replace(".", File.separator) + File.separator + "controller" + File.separator + tableInfo.getEntityName() + "Controller" + StringPool.DOT_JAVA;
            }
        });
        foc.add(new FileOutConfig(File.separator + "templates" + File.separator + "addModel.java.vm" ) {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return finalPath + tableInfo.getEntityPath() + File.separator + tableInfo.getEntityName() + "Add"
                        + StringPool.DOT_JAVA;
            }
        });
        //添加update 请求对象
        foc.add(new FileOutConfig(File.separator + "templates" + File.separator + "updateModel.java.vm" ) {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return finalPath + tableInfo.getEntityPath() + File.separator + tableInfo.getEntityName() + "Update"
                        + StringPool.DOT_JAVA;
            }
        });
        //添加list请求对象
        foc.add(new FileOutConfig(File.separator + "templates" + File.separator + "detailModel.java.vm" ) {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return finalPath + tableInfo.getEntityPath() + File.separator + tableInfo.getEntityName() + "Detail"
                        + StringPool.DOT_JAVA;
            }
        });
        //添加分页请求对象
        foc.add(new FileOutConfig(File.separator + "templates" + File.separator + "pageModel.java.vm" ) {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return finalPath + tableInfo.getEntityPath() + File.separator + tableInfo.getEntityName() + "Page"
                        + StringPool.DOT_JAVA;
            }
        });

        //添加list页面请求对象
        foc.add(new FileOutConfig(File.separator + "templates" + File.separator + "listPage.html.vm" ) {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return demo_path + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "templates" + File.separator + tableInfo.getEntityPath() + File.separator + "list.html";
            }
        });
        cfg.setFileOutConfigList(foc);
        mpg.setCfg(cfg);
        // 关闭默认 xml 生成，调整生成 至 根目录
        TemplateConfig tc = new TemplateConfig();
        tc.setXml(null);
        tc.setController(null);
        mpg.setTemplate(tc);
        // 执行生成
        mpg.execute();

        // 打印注入设置
        System.err.println(mpg.getCfg().getMap().get("abc" ));

    }

}
