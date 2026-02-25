package com.workforce.upload.service;

import com.workforce.upload.domain.UploadJob;
import com.workforce.upload.dto.response.UploadJobResponse;
import com.workforce.upload.exception.InvalidFileException;
import com.workforce.upload.repository.UploadJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;

class UploadServiceImplTest {

    private UploadServiceImpl service;
    private UploadJobRepository repo;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(UploadJobRepository.class);
        service = new UploadServiceImpl(repo, UploadJobMapper.INSTANCE);
        // stub save to return the passed job with an id
        Mockito.when(repo.save(Mockito.any(UploadJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenCsvFileProvided_initiateUploadSucceeds() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.csv",
                "text/csv",
                "first_name,last_name\nJohn,Doe".getBytes()
        );

        UploadJobResponse response = service.initiateUpload(file, "test csv");
        assertThat(response).isNotNull();
        assertThat(response.getFileType()).isEqualTo(UploadJob.FileType.CSV);
        assertThat(response.getOriginalFilename()).isEqualTo("employees.csv");
    }

    @Test
    void whenNonCsvFileProvided_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                new byte[]{0,1,2}
        );

        assertThatThrownBy(() -> service.initiateUpload(file, "bad"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Invalid file type");
    }
}
