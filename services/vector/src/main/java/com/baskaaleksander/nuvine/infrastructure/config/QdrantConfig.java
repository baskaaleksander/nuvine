package com.baskaaleksander.nuvine.infrastructure.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    @Bean
    public QdrantClient qdrantClient(QdrantProperties props) {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient
                .newBuilder(props.host(), props.port());

        if (props.apiKey() != null) {
            builder = builder.withApiKey(props.apiKey());
        }

        return new QdrantClient(builder.build());
    }

    @Bean
    @ConfigurationProperties(prefix = "qdrant")
    public QdrantProperties qdrantProperties() {
        return new QdrantProperties();
    }

    public static class QdrantProperties {
        private String host;
        private int port;
        private String apiKey;
        private String collection;

        public String host() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int port() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String apiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String collection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }
    }
}
