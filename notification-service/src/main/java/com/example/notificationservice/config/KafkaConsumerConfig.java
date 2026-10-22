package com.example.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    /**
     * What to do when processing a message throws an exception (for example the database is briefly down):
     * try again 3 more times, 1 second apart. If it still fails, log the error and move on to the next message,
     * so one bad message can never block the topic forever.
     * Spring Boot picks this bean up automatically for every @KafkaListener.
     * (A production setup would also send the failed message to a "dead letter topic".)
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(1000L, 3L));
    }
}
