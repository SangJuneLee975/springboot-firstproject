package com.example.apitest.controller;

import com.example.apitest.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class ImageController {

    private final S3Service s3Service;

    public ImageController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("key") String key) {
        try {
            return s3Service.upload(file, key);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("파일 업로드 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam("key") String key) {
        return s3Service.download(key);
    }
}