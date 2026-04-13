package com.focuskids.trainer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.boot.starter.autoconfigure.OpenApiAutoConfiguration;

@SpringBootApplication(exclude = {OpenApiAutoConfiguration.class})
@MapperScan("com.focuskids.trainer.mapper")
@EnableScheduling
public class FocusTrainerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FocusTrainerApplication.class, args);
    }
}
