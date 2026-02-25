package com.workforce.upload.controller;

import com.workforce.upload.dto.response.UploadJobResponse;
import com.workforce.upload.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "Workforce file upload operations")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a workforce file",
            description = "Accepts CSV or Excel files for async workforce data processing",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Upload accepted"),
                    @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content(schema = @Schema(implementation = com.workforce.upload.dto.response.ErrorResponse.class)))
            }
    )
    public ResponseEntity<UploadJobResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "description", required = false) String description) {
        log.info("Received upload request for file: {}", file.getOriginalFilename());
        UploadJobResponse response = uploadService.initiateUpload(file, description);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get upload job by ID")
    public ResponseEntity<UploadJobResponse> getUploadJob(
            @Parameter(description = "Upload job UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(uploadService.getUploadJob(id));
    }

    @GetMapping
    @Operation(summary = "List all upload jobs with pagination")
    public ResponseEntity<Page<UploadJobResponse>> getAllUploadJobs(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(uploadService.getAllUploadJobs(pageable));
    }
}
