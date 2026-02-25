package com.workforce.query.repository;

import com.workforce.query.domain.EmployeeView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeViewRepository extends JpaRepository<EmployeeView, UUID> {

    Optional<EmployeeView> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<EmployeeView> findAllByTenantIdAndStatusAndDeletedFalse(String tenantId, EmployeeView.EmployeeStatus status, Pageable pageable);

    Page<EmployeeView> findAllByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);
}
