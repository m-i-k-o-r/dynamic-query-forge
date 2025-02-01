package com.koroli.dynamicqueryforge;

import com.koroli.dynamicqueryforge.annotation.EnableDynamicQueryRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDynamicQueryRepositories(basePackages = "com.koroli.dynamicqueryforge")
public class DynamicQueryForgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(DynamicQueryForgeApplication.class, args);
    }
}
