package com.treserve.storage;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientMinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @CircuitBreaker(name = "minio", fallbackMethod = "uploadToMinioFallback")
    public String uploadToMinio(String key, byte[] content, String contentType) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .stream(inputStream, content.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("📤 Uploaded to MinIO: {}", key);
            return key;
        } catch (Exception e) {
            log.error("❌ MinIO upload failed: {}", e.getMessage());
            throw new RuntimeException("MinIO upload failed", e);
        }
    }

    @SuppressWarnings("unused")
    private String uploadToMinioFallback(String key, byte[] content, String contentType, Exception e) {
        log.warn("⚠️ Circuit Breaker OPEN — saving PDF locally instead of MinIO: {}", key);
        
        // Сохраняем локально как fallback
        try {
            String localPath = "/tmp/fallback_" + key.replace("/", "_");
            java.nio.file.Files.write(java.nio.file.Paths.get(localPath), content);
            log.info("💾 PDF saved locally as fallback: {}", localPath);
            return "local://" + key;
        } catch (Exception ex) {
            log.error("❌ Even local fallback failed: {}", ex.getMessage());
            return "failed://" + key;
        }
    }
}