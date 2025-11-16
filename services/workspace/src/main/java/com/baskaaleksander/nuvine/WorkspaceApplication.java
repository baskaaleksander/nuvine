package com.baskaaleksander.nuvine;

import com.baskaaleksander.nuvine.infrastructure.auth.KeycloakClientCredentialsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableConfigurationProperties(KeycloakClientCredentialsProperties.class)
@EnableFeignClients
public class WorkspaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkspaceApplication.class, args);
    }

}
