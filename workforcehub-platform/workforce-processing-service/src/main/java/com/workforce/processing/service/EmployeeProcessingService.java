package com.workforce.processing.service;

import com.workforce.processing.config.KafkaTopics;
import com.workforce.processing.domain.Employee;
import com.workforce.processing.dto.request.EmployeeUploadRequestedEvent;
import com.workforce.processing.dto.response.EmployeeUploadProcessedEvent;
import com.workforce.processing.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeProcessingService {

    private static final int BATCH_SIZE = 100;

    private final EmployeeRepository employeeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void processUpload(EmployeeUploadRequestedEvent event) {
        // NOTE: In full implementation (Prompt 3+), this reads from storage.
        // For Prompt 1, we demonstrate the batch-insert pattern with mock data.
        log.info("Processing upload job: {}", event.getUploadJobId());

        List<Employee> batch = new ArrayList<>();
        int processed = 0;
        int failed = 0;

        // Simulate processing (real impl streams CSV/Excel from object storage)
        List<Employee> mockEmployees = buildMockEmployees(event.getUploadJobId(), 10);

        for (Employee employee : mockEmployees) {
            try {
                batch.add(employee);
                if (batch.size() >= BATCH_SIZE) {
                    employeeRepository.saveAll(batch);
                    processed += batch.size();
                    batch.clear();
                    log.debug("Flushed batch of {} employees for job {}", BATCH_SIZE, event.getUploadJobId());
                }
            } catch (Exception e) {
                log.warn("Failed to process employee row: {}", e.getMessage());
                failed++;
            }
        }

        // Flush remaining
        if (!batch.isEmpty()) {
            employeeRepository.saveAll(batch);
            processed += batch.size();
        }

        publishProcessedEvent(event, processed, failed);
    }

    private void publishProcessedEvent(EmployeeUploadRequestedEvent event, int processed, int failed) {
        EmployeeUploadProcessedEvent processedEvent = EmployeeUploadProcessedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventVersion("1.0")
                .uploadJobId(event.getUploadJobId())
                .totalRows(processed + failed)
                .processedRows(processed)
                .failedRows(failed)
                .status(failed == 0 ? "COMPLETED" : "PARTIAL")
                .processedAt(Instant.now())
                .build();

        kafkaTemplate.send(KafkaTopics.EMPLOYEE_UPLOAD_PROCESSED,
                event.getUploadJobId().toString(), processedEvent);
        log.info("Published processed event for job {}: processed={}, failed={}", event.getUploadJobId(), processed, failed);
    }

    private List<Employee> buildMockEmployees(UUID uploadJobId, int count) {
        List<Employee> employees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            employees.add(Employee.builder()
                    .uploadJobId(uploadJobId)
                    .firstName("First" + i)
                    .lastName("Last" + i)
                    .email("employee" + i + "_" + UUID.randomUUID() + "@example.com")
                    .department("Engineering")
                    .jobTitle("Developer")
                    .status(Employee.EmployeeStatus.ACTIVE)
                    .build());
        }
        return employees;
    }
}
