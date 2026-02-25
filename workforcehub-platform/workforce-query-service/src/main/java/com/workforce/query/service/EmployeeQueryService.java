package com.workforce.query.service;

import com.workforce.query.config.CacheConfig;
import com.workforce.query.domain.EmployeeView;
import com.workforce.query.dto.response.EmployeeResponse;
import com.workforce.query.exception.EmployeeNotFoundException;
import com.workforce.query.mapper.EmployeeMapper;
import com.workforce.query.repository.EmployeeViewRepository;
import com.workforce.query.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeQueryService {

    private final EmployeeViewRepository repository;
    private final EmployeeMapper mapper;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.EMPLOYEE_CACHE, key = "#root.target.getTenantId() + ':' + #id")
    public EmployeeResponse getById(UUID id) {
        return repository.findByIdAndTenantIdAndDeletedFalse(id, TenantContext.getTenantId())
                .map(mapper::toResponse)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAll(String status, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        if (status != null && !status.isBlank()) {
            EmployeeView.EmployeeStatus employeeStatus = EmployeeView.EmployeeStatus.valueOf(status.toUpperCase());
            return repository.findAllByTenantIdAndStatusAndDeletedFalse(tenantId, employeeStatus, pageable)
                    .map(mapper::toResponse);
        }
        return repository.findAllByTenantIdAndDeletedFalse(tenantId, pageable)
                .map(mapper::toResponse);
    }

    // Used by SpEL in @Cacheable key — keeps cache keys tenant-scoped
    public String getTenantId() {
        return TenantContext.getTenantId();
    }
}
