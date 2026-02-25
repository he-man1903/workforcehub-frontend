package com.workforce.processing.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic uploadRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.EMPLOYEE_UPLOAD_REQUESTED)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic uploadProcessedTopic() {
        return TopicBuilder.name(KafkaTopics.EMPLOYEE_UPLOAD_PROCESSED)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic uploadFailedTopic() {
        return TopicBuilder.name(KafkaTopics.EMPLOYEE_UPLOAD_FAILED)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic uploadRequestedDltTopic() {
        return TopicBuilder.name(KafkaTopics.EMPLOYEE_UPLOAD_REQUESTED + ".DLT")
                .partitions(1).replicas(1).build();
    }

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(30000L); // max 30s total retry

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
