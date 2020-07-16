package ru.pochta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.pochta.config.YmlProperties;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableConfigurationProperties(YmlProperties.class)
@EnableScheduling
public class SftpImporterApp {
    public static void main(String[] args) {
        SpringApplication.run(SftpImporterApp.class);
    }
}
