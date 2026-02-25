package com.workforce.upload.exception;

import java.util.UUID;

public class UploadJobNotFoundException extends RuntimeException {

    public UploadJobNotFoundException(UUID id) {
        super("Upload job not found with id: " + id);
    }
}
