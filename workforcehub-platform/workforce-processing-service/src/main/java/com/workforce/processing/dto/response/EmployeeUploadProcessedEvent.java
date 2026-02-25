package com.workforce.processing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUploadProcessedEvent {
    private String eventId;
    private String eventVersion;
    private UUID uploadJobId;
    private int totalRows;
    private int processedRows;
    private int failedRows;
    private String status;
    private Instant processedAt;
}
