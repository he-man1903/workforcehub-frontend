package com.workforce.upload.service;

import com.workforce.upload.domain.UploadJob;
import com.workforce.upload.dto.response.UploadJobResponse;
import com.workforce.upload.exception.InvalidFileException;
import com.workforce.upload.exception.UploadJobNotFoundException;
import com.workforce.upload.mapper.UploadJobMapper;
import com.workforce.upload.repository.UploadJobRepository;
import com.workforce.upload.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "text/csv",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final UploadJobRepository uploadJobRepository;
    private final UploadJobMapper uploadJobMapper;

    @Override
    @Transactional
    public UploadJobResponse initiateUpload(MultipartFile file, String description) {
        log.info("Initiating upload for file: {}, size: {} bytes, tenant: {}", file.getOriginalFilename(), file.getSize(), TenantContext.getTenantId());

        validateFile(file);

        UploadJob.FileType fileType = resolveFileType(file);

        UploadJob job = UploadJob.builder()
                .tenantId(TenantContext.getTenantId())
                .originalFilename(file.getOriginalFilename())
                .fileType(fileType)
                .status(UploadJob.UploadStatus.PENDING)
                .processedRows(0)
                .failedRows(0)
                .build();

        UploadJob saved = uploadJobRepository.save(job);
        log.info("Upload job created with id: {}", saved.getId());

        return uploadJobMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UploadJobResponse getUploadJob(UUID id) {
        return uploadJobRepository.findByIdAndTenantIdAndDeletedFalse(id, TenantContext.getTenantId())
                .map(uploadJobMapper::toResponse)
                .orElseThrow(() -> new UploadJobNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UploadJobResponse> getAllUploadJobs(Pageable pageable) {
        return uploadJobRepository.findAllByTenantIdAndDeletedFalse(TenantContext.getTenantId(), pageable)
                .map(uploadJobMapper::toResponse);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileException("Invalid file type. Only CSV and Excel files are supported.");
        }
    }

    private UploadJob.FileType resolveFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if ("text/csv".equals(contentType)) {
            return UploadJob.FileType.CSV;
        }
        return UploadJob.FileType.EXCEL;
    }
}
