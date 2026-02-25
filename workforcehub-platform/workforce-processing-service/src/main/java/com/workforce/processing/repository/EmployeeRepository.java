package com.workforce.processing.repository;

import com.workforce.processing.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmailAndDeletedFalse(String email);

    List<Employee> findByUploadJobId(UUID uploadJobId);
}
