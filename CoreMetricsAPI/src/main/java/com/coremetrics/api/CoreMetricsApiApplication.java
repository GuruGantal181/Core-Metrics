package com.coremetrics.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.zip.DataFormatException;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@RestController
public class CoreMetricsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreMetricsApiApplication.class, args);
    }
    @GetMapping
    public String helloWorld(){
        return "hello Spring boot ";
    }
}
