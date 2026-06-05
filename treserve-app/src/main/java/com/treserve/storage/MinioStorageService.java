package com.treserve.storage;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public String uploadFile(String key, byte[] content, String contentType) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .stream(inputStream, content.length, -1)
                    .contentType(contentType)
                    .build());
            
            log.info("Uploaded file: {}", key);
            return key;
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload PDF to storage", e);
        }
    }

    @Override
    public InputStream downloadFile(String key) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .build());
        } catch (Exception e) {
            log.error("Failed to download file: {}", e.getMessage());
            throw new RuntimeException("Failed to download PDF from storage", e);
        }
    }

    @Override
    public String getDownloadUrl(String key) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate download URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    @Override
    public boolean fileExists(String key) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}