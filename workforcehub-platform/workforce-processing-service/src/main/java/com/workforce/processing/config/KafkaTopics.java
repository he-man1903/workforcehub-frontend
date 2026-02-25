package com.workforce.processing.config;

public final class KafkaTopics {
    public static final String EMPLOYEE_UPLOAD_REQUESTED = "employee.upload.requested";
    public static final String EMPLOYEE_UPLOAD_PROCESSED = "employee.upload.processed";
    public static final String EMPLOYEE_UPLOAD_FAILED    = "employee.upload.failed";

    private KafkaTopics() {}
}
