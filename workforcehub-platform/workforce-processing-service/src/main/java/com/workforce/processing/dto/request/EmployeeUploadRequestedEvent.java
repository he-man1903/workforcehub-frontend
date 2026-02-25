package com.workforce.processing.dto.request;

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
public class EmployeeUploadRequestedEvent {
    private String eventId;
    private String eventVersion;
    private UUID uploadJobId;
    private String originalFilename;
    private String fileType;
    private Instant requestedAt;
    private String requestedBy;
}
