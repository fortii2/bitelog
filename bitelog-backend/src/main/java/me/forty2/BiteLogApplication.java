package me.forty2;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("me.forty2.mapper")
@SpringBootApplication
public class BiteLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiteLogApplication.class, args);
    }

}
