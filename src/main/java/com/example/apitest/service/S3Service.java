package com.example.apitest.service;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.s3.S3Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class S3Service {
    private static final String BUCKET = System.getenv("AWS_S3_BUCKET");
    private final S3Operations s3Operations;

    public S3Service(S3Operations s3Operations) {
        this.s3Operations = s3Operations;
    }

    @Transactional
    public ResponseEntity<?> upload(MultipartFile multipartFile, String key) throws IOException {
        if (!MediaType.IMAGE_PNG.toString().equals(multipartFile.getContentType()) &&
                !MediaType.IMAGE_JPEG.toString().equals(multipartFile.getContentType())) {
            return ResponseEntity.badRequest().body("사진 파일만 업로드 가능합니다");
        }

        try (InputStream is = multipartFile.getInputStream()) {
            s3Operations.upload(BUCKET, key, is,
                    ObjectMetadata.builder().contentType(multipartFile.getContentType()).build());
        }

        return ResponseEntity.accepted().build();
    }

    @Transactional
    public ResponseEntity<?> download(@RequestParam String key) {
        S3Resource s3Resource = s3Operations.download(BUCKET, key);

        if (MediaType.IMAGE_PNG.toString().equals(s3Resource.contentType())) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(s3Resource);
        }

        if (MediaType.IMAGE_JPEG.toString().equals(s3Resource.contentType())) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(s3Resource);
        }

        return ResponseEntity.badRequest().body("사진 파일만 다운로드 가능합니다");
    }
}