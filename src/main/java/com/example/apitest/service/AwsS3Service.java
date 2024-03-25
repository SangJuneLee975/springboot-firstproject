package com.example.apitest.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.example.apitest.repository.ImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.example.apitest.DTO.Image;
import java.time.LocalDateTime;

import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Service
public class AwsS3Service {

    private final AmazonS3 amazonS3Client;

    private final ImageRepository imageRepository;


    @Autowired
    public AwsS3Service(AmazonS3 amazonS3Client, ImageRepository imageRepository)
    {
        this.amazonS3Client = amazonS3Client;
        this.imageRepository = imageRepository;

    }


    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    // SLF4J 로거 인스턴스 추가
    private static final Logger logger = LoggerFactory.getLogger(AwsS3Service.class);



    // 여러 파일을 S3에 업로드하고, 업로드된 파일의 URL 목록을 반환하는 메서드
    public List<String> uploadMultipleFiles(List<MultipartFile> multipartFiles) {
        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : multipartFiles) {
            try {
                // 기존의 단일 파일 업로드 메서드를 사용하여 파일을 업로드하고 URL을 받아옴
                String fileUrl = uploadFileToS3(file);
                fileUrls.add(fileUrl);
            } catch (IOException e) {
                logger.error("Error uploading file to S3: {}", file.getOriginalFilename(), e);
                // 실패한 파일에 대한 처리를 여기에서 할 수 있습니다. 예를 들면, 다시 시도할 수도 있습니다.
            }
        }
        return fileUrls; // 업로드된 파일 URL들의 목록 반환
    }


    // 파일 S3 버킷에 업로드 하는 메소드
    // 파일 S3 버킷에 업로드하는 메소드 및 업로드된 파일 URL을 DB에 저장

    public String uploadFileToS3(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            throw new IOException("업로드할 파일이 비어 있습니다.");
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uuid = UUID.randomUUID().toString();
        String fileName = "board-image/" + uuid + extension;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(multipartFile.getContentType());
        metadata.setContentLength(multipartFile.getSize());
        logger.info("Uploading file to S3: {}", fileName);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            amazonS3Client.putObject(bucketName, fileName, inputStream, metadata);
            String fileUrl = amazonS3Client.getUrl(bucketName, fileName).toString();
            logger.info("File uploaded successfully to S3. URL: {}", fileUrl);
            return fileUrl;
        } catch (AmazonServiceException e) {
            logger.error("AWS service error: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.error("File IO error: {}", e.getMessage());
            throw e;
        }
    }


    // S3 버킷에서 특정 폴더의 모든 파일 URL을 반환하는 메소드
    public List<String> listFiles(String folderKey) {
        List<String> fileUrls = new ArrayList<>();
        ListObjectsV2Request req = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(folderKey + "/"); // 폴더 경로를 명시합니다.

        ListObjectsV2Result result;

        do {
            result = amazonS3Client.listObjectsV2(req);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                // S3 객체의 전체 경로를 가져옵니다.
                String fileUrl = amazonS3Client.getUrl(bucketName, objectSummary.getKey()).toString();
                fileUrls.add(fileUrl);
            }
            // 다음 페이지의 객체를 가져오기 위한 연속 토큰을 설정합니다.
            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated()); // 결과가 더 있으면 계속합니다.

        return fileUrls;
    }



}




//    // S3에 파일을 업로드하고 파일 URL을 반환하는 메서드
//    private String uploadToS3(MultipartFile multipartFile, String filePath) throws IOException {
//        ObjectMetadata metadata = new ObjectMetadata();
//        metadata.setContentType(multipartFile.getContentType());
//        metadata.setContentLength(multipartFile.getSize());
//
//        try (InputStream inputStream = multipartFile.getInputStream()) {
//            amazonS3Client.putObject(new PutObjectRequest(bucketName, filePath, inputStream, metadata).withCannedAcl(CannedAccessControlList.PublicRead));
//            return amazonS3Client.getUrl(bucketName, filePath).toString();
//        } catch (AmazonServiceException e) {
//            // AWS 서비스 오류 발생 시 로그 남기기
//            logger.error("AWS 서비스 오류: {}", e.getMessage());
//            throw e;
//        } catch (IOException e) {
//            // 파일 입출력 오류 발생 시 로그 남기기
//            logger.error("파일 입출력 오류: {}", e.getMessage());
//            throw e;
//        }
//    }


//    // 파일 S3 버킷에 업로드 하는 메소드
//    public String uploadFileToS3(MultipartFile multipartFile) throws IOException {
//        if (multipartFile.isEmpty()) {
//            throw new IOException("업로드할 파일이 비어 있습니다.");
//        }
//
//        String key = System.currentTimeMillis() + "_" + multipartFile.getOriginalFilename();
//        ObjectMetadata metadata = new ObjectMetadata();
//        metadata.setContentType(multipartFile.getContentType());
//        metadata.setContentLength(multipartFile.getSize());
//
//        // 파일 업로드 시작 로깅
//        logger.info("Uploading file to S3: {}", key);
//
//        try (InputStream inputStream = multipartFile.getInputStream()) {
//
//            amazonS3Client.putObject(bucketName, key, inputStream, metadata);
//
//            String fileUrl = amazonS3Client.getUrl(bucketName, key).toString();
//            logger.info("File uploaded successfully to S3. URL: {}", fileUrl);
//
//            return fileUrl;
//        } catch (Exception e) {
//            // 파일 업로드 실패 로깅
//            logger.error("Error uploading file to S3: {}", e.getMessage());
//            throw e;
//        }
//    }






//
//    // S3 객체의 URL을 가져오는 메소드
//    public String getUrl(String bucket, String fileName) {
//        return amazonS3Client.getUrl(bucket, fileName).toString();
//    }
//
//    //S3폴더 내 파일 리스트 전달
//    public List<String> listFiles(String folderKey) {
//        List<String> fileUrls = new ArrayList<>();
//        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withPrefix(folderKey+"/");
//        ListObjectsV2Result result;
//
//        do {
//            result = amazonS3Client.listObjectsV2(req);
//
//            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
//                String fileUrl = getUrl(bucketName, objectSummary.getKey());
//                fileUrls.add(fileUrl);
//            }
//            req.setContinuationToken(result.getNextContinuationToken());
//        } while (result.isTruncated());
//
//        return fileUrls;
//    }







