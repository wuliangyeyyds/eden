package com.ssrms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ssrms.mapper")
public class SsrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsrmsApplication.class, args);
    }

}
