package com.rj.issuehelper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.rj.issuehelper.mapper")
public class IssueHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(IssueHelperApplication.class, args);
    }
}
