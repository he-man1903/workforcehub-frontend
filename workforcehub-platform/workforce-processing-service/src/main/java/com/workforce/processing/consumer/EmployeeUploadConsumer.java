package com.workforce.processing.consumer;

import com.workforce.processing.config.KafkaTopics;
import com.workforce.processing.dto.request.EmployeeUploadRequestedEvent;
import com.workforce.processing.service.EmployeeProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeUploadConsumer {

    private final EmployeeProcessingService processingService;

    @KafkaListener(
            topics = KafkaTopics.EMPLOYEE_UPLOAD_REQUESTED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUploadRequested(
            @Payload EmployeeUploadRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        MDC.put("correlationId", event.getEventId());
        MDC.put("uploadJobId", event.getUploadJobId().toString());

        try {
            log.info("Received upload event: jobId={}, file={}, partition={}, offset={}",
                    event.getUploadJobId(), event.getOriginalFilename(), partition, offset);
            processingService.processUpload(event);
            log.info("Successfully processed upload event: jobId={}", event.getUploadJobId());
        } finally {
            MDC.clear();
        }
    }
}
