package com.aigc.contentfactory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.aigc.contentfactory.mapper")
public class AigcContentFactoryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AigcContentFactoryServerApplication.class, args);
    }
}
