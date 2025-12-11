package com.ssrms.common;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.nio.file.Paths;
import java.util.Collections;

/**
 * MyBatis-Plus 代码生成器
 * 依赖版本：
 *   - mybatis-plus-spring-boot4-starter 3.5.14
 *   - mybatis-plus-generator 3.5.14
 *   - freemarker 2.3.34
 */
public class CodeGenerator {

    // 1. 数据源配置
    private static final String URL =
            "jdbc:mysql://localhost:3306/ssrms"
            + "?useUnicode=true&characterEncoding=utf-8"
            + "&useSSL=false&serverTimezone=GMT%2B8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "fzh586176";

    // 要生成的表名
    private static final String[] TABLES = {"user"};

    public static void main(String[] args) {

        // 项目路径
        String projectPath = System.getProperty("user.dir") + "/SSRMS";
        String javaOutputDir = Paths.get(projectPath, "src", "main", "java").toString();
        String mapperXmlOutputDir = Paths.get(projectPath, "src", "main", "resources", "mapper").toString();

        FastAutoGenerator.create(URL, USERNAME, PASSWORD)
                // 2. 全局配置
                .globalConfig(builder -> builder
                        .author("SSRMS")                 // 作者名
                        .commentDate("yyyy-MM-dd")       // 注释上的日期格式
                        .outputDir(javaOutputDir)        // Java 文件输出路径
                        .disableOpenDir()                // 生成完不自动打开目录
                )
                // 3. 包配置
                .packageConfig(builder -> builder
                        .parent("com.ssrms")             // 父包
                        .entity("entity")                // 实体类包名：com.ssrms.entity
                        .mapper("mapper")                // Mapper 接口包名
                        .service("service")              // Service 接口包名
                        .serviceImpl("service.impl")     // Service 实现类包名
                        .controller("controller")        // Controller 包名
                        // 指定 mapper.xml 输出到 resources/mapper 目录
                        .pathInfo(Collections.singletonMap(
                                OutputFile.xml, mapperXmlOutputDir))
                )
                // 4. 策略配置
                .strategyConfig(builder -> {
                    // 要生成的表
                    builder.addInclude(TABLES)
                            // 过滤掉表前缀（如果你表叫 t_user，就写 "t_"）
                            .addTablePrefix("t_", "sys_");

                    // entity 策略
                    builder.entityBuilder()
                            .enableLombok()              // 实体类加 @Data 等 Lombok 注解
                            .enableTableFieldAnnotation(); // 字段上生成 @TableField 等注解

                    // controller 策略
                    builder.controllerBuilder()
                            .enableRestStyle()            // @RestController 风格
                            .formatFileName("%sController");

                    // service 策略
                    builder.serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl");

                    // mapper 策略
                    builder.mapperBuilder()
                            .enableBaseResultMap()        // 生成通用的 resultMap
                            .enableBaseColumnList()       // 生成通用的 columnList
                            .formatMapperFileName("%sMapper")
                            .formatXmlFileName("%sMapper");
                })
                // 5. 模板引擎
                .templateEngine(new FreemarkerTemplateEngine())
                // 6. 执行生成
                .execute();
    }
}
