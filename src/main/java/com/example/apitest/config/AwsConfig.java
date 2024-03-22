package com.example.apitest.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;



@Configuration
public class AwsConfig {
    @Value("${cloud.aws.credentials.accessKey}")
    private String AwsaccessKey;
    @Value("${cloud.aws.credentials.secretKey}")
    private String AwssecretKey;
    @Value("${cloud.aws.region.static}")
    private String region;

    // access, secret key 이용해 aws 자격증명 제공
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        AwsCredentials awsCredentials = AwsBasicCredentials.create(AwsaccessKey, AwssecretKey);
        return StaticCredentialsProvider.create(awsCredentials);
    }
//    // s3서비스를 이용하기 위한 S3Client 객체 생성
    @Bean
    public AmazonS3 amazonS3Client() {
        return AmazonS3ClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_2)
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AwsaccessKey, AwssecretKey)))
            .build();
}
    // Pre-signed Url을 적용하기 위한 S3Presigner 객체 생성
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(awsCredentialsProvider())
                .build();
    }


}