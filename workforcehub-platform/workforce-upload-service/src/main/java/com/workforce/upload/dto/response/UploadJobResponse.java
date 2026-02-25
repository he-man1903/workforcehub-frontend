package com.workforce.upload.dto.response;

import com.workforce.upload.domain.UploadJob;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UploadJobResponse {
    private UUID id;
    private String originalFilename;
    private UploadJob.FileType fileType;
    private UploadJob.UploadStatus status;
    private Integer totalRows;
    private Integer processedRows;
    private Integer failedRows;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}
