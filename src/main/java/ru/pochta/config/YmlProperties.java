package ru.pochta.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter

@ConfigurationProperties(prefix = "sftp")
public class YmlProperties {
    private String hostname;
    private String username;
    private String password;
    private String remoteFilesDirPath;
    private String localFilesDirPath;
    private long fixedRateValue;
}
