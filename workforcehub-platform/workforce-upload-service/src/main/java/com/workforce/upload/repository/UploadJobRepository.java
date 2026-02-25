package com.workforce.upload.repository;

import com.workforce.upload.domain.UploadJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadJobRepository extends JpaRepository<UploadJob, UUID> {

    Optional<UploadJob> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<UploadJob> findAllByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);
}
