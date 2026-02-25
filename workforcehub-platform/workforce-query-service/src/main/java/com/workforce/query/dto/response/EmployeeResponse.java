package com.workforce.query.dto.response;

import com.workforce.query.domain.EmployeeView;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class EmployeeResponse {
    private UUID id;
    private UUID uploadJobId;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String jobTitle;
    private LocalDate hireDate;
    private EmployeeView.EmployeeStatus status;
    private Instant createdAt;
}
