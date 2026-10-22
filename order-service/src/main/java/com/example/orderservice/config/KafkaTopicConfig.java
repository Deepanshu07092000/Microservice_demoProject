package com.example.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    /**
     * Spring Boot creates this topic on startup if it does not exist yet.
     *
     * partitions = 3 : the topic is split into 3 lanes so several consumers could share the work.
     *                  Messages with the same key (we use the orderId) always go to the same lane,
     *                  which keeps the events of ONE order in the correct order.
     * replicas   = 1 : we run a single Kafka broker, so there is nowhere to keep a second copy.
     */
    @Bean
    public NewTopic orderEventsTopic(@Value("${app.kafka.topic.order-events}") String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
