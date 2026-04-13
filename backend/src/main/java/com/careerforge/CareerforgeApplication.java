package com.careerforge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CareerForge 智能求职助手应用程序入口
 *
 * @author Azir
 */
@SpringBootApplication
@MapperScan("com.careerforge.**.mapper")
public class CareerforgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerforgeApplication.class, args);
    }

}
