package com.platform.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.time.Duration;

@Configuration
public class AiHttpConfig {
    @Bean
    RestClient.Builder aiRestClientBuilder(
            @Value("${app.ai.connect-timeout}") Duration connectTimeout,
            @Value("${app.ai.read-timeout}") Duration readTimeout,
            @Value("${app.ai.proxy.enabled:false}") boolean proxyEnabled,
            @Value("${app.ai.proxy.host:127.0.0.1}") String proxyHost,
            @Value("${app.ai.proxy.port:7897}") int proxyPort) {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().connectTimeout(connectTimeout);
        if (proxyEnabled) {
            httpClientBuilder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
        }
        HttpClient httpClient = httpClientBuilder.build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(factory);
    }
}
