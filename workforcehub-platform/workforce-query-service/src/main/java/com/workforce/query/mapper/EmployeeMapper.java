package com.workforce.query.mapper;

import com.workforce.query.domain.EmployeeView;
import com.workforce.query.dto.response.EmployeeResponse;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(EmployeeView view) {
        return EmployeeResponse.builder()
                .id(view.getId())
                .uploadJobId(view.getUploadJobId())
                .firstName(view.getFirstName())
                .lastName(view.getLastName())
                .email(view.getEmail())
                .department(view.getDepartment())
                .jobTitle(view.getJobTitle())
                .hireDate(view.getHireDate())
                .status(view.getStatus())
                .createdAt(view.getCreatedAt())
                .build();
    }
}
