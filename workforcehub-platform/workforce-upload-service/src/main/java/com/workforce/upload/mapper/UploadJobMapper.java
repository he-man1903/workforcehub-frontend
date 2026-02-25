package com.workforce.upload.mapper;

import com.workforce.upload.domain.UploadJob;
import com.workforce.upload.dto.response.UploadJobResponse;
import org.springframework.stereotype.Component;

@Component
public class UploadJobMapper {

    public UploadJobResponse toResponse(UploadJob job) {
        return UploadJobResponse.builder()
                .id(job.getId())
                .originalFilename(job.getOriginalFilename())
                .fileType(job.getFileType())
                .status(job.getStatus())
                .totalRows(job.getTotalRows())
                .processedRows(job.getProcessedRows())
                .failedRows(job.getFailedRows())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
