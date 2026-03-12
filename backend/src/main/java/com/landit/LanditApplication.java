package com.landit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LandIt 智能求职助手应用程序入口
 *
 * @author Azir
 */
@SpringBootApplication
@MapperScan("com.landit.**.mapper")
public class LanditApplication {

    public static void main(String[] args) {
        SpringApplication.run(LanditApplication.class, args);
    }

}
