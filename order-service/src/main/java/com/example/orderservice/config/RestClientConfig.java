package com.example.orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * @LoadBalanced lets this builder understand service names like "http://USER-SERVICE/...".
     * It asks Eureka for the real host:port and load-balances between instances.
     * The timeouts stop Order Service from waiting forever on a slow User Service
     * (same idea as the Feign timeouts we set earlier).
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);      // ms to establish the connection
        factory.setReadTimeout(3000);        // ms to wait for the response
        return RestClient.builder();
    }
}
