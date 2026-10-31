package com.bishe.teach;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bishe.teach.mapper")
public class TeachEvalApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeachEvalApplication.class, args);
    }
}
