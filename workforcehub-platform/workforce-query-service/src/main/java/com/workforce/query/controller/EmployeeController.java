package com.workforce.query.controller;

import com.workforce.query.dto.response.EmployeeResponse;
import com.workforce.query.service.EmployeeQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Workforce employee query operations")
public class EmployeeController {

    private final EmployeeQueryService queryService;

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<EmployeeResponse> getById(
            @Parameter(description = "Employee UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(queryService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List employees with optional status filter and pagination")
    public ResponseEntity<Page<EmployeeResponse>> getAll(
            @Parameter(description = "Filter by status: ACTIVE, INACTIVE, PENDING")
            @RequestParam(required = false) String status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(queryService.getAll(status, pageable));
    }
}
