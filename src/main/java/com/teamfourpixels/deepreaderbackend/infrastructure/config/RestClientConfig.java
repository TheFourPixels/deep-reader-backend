package com.teamfourpixels.deepreaderbackend.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    public static ClientHttpRequestFactory createRequestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    @Bean("openAiRequestFactory")
    public ClientHttpRequestFactory openAiRequestFactory(AiProperties aiProperties) {
        int timeoutSec = aiProperties.getOpenai() != null && aiProperties.getOpenai().getTimeoutSeconds() > 0
                ? aiProperties.getOpenai().getTimeoutSeconds()
                : 12;
        return createRequestFactory(Duration.ofSeconds(5), Duration.ofSeconds(timeoutSec));
    }

    @Bean("deepLRequestFactory")
    public ClientHttpRequestFactory deepLRequestFactory(AiProperties aiProperties) {
        int timeoutSec = aiProperties.getDeepl() != null && aiProperties.getDeepl().getTimeoutSeconds() > 0
                ? aiProperties.getDeepl().getTimeoutSeconds()
                : 3;
        return createRequestFactory(Duration.ofSeconds(2), Duration.ofSeconds(timeoutSec));
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
