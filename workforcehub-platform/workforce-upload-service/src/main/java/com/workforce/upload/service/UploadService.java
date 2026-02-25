package com.workforce.upload.service;

import com.workforce.upload.dto.response.UploadJobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UploadService {

    UploadJobResponse initiateUpload(MultipartFile file, String description);

    UploadJobResponse getUploadJob(UUID id);

    Page<UploadJobResponse> getAllUploadJobs(Pageable pageable);
}
