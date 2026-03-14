package com.example.cookiemusicdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static com.example.cookiemusicdemo.constant.Constants.ASSETS_PATH;

@SpringBootApplication
@MapperScan("com.example.cookiemusicdemo.mapper")
public class CookieMusicDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CookieMusicDemoApplication.class, args);
    }

}