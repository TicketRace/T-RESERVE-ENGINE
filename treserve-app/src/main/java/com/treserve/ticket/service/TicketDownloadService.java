package com.treserve.ticket.service;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ResourceNotFoundException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketDownloadService {

    private final TicketRepository ticketRepository;
    private final TicketPdfGenerator pdfGenerator;
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Transactional
    public byte[] getOrCreateTicketPdf(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        // Проверка прав — используем userId из билета
        if (ticket.getUserId() == null || !ticket.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Ticket does not belong to user");
        }

        // Проверка статуса
        if (ticket.getStatus() != TicketStatus.BOOKED) {
            throw new IllegalStateException("Ticket is not confirmed (status: " + ticket.getStatus() + ")");
        }

        // Если PDF уже есть — скачиваем
        if (ticket.getPdfUrl() != null) {
            log.info("PDF already exists for ticket {}, downloading from MinIO", ticketId);
            try {
                return downloadFromMinio(ticket.getPdfUrl());
            } catch (Exception e) {
                log.error("Failed to download PDF from MinIO: {}", e.getMessage());
            }
        }

        // Генерация нового PDF
        log.info("Generating PDF for ticket {}", ticketId);
        byte[] pdfBytes = pdfGenerator.generatePdf(ticket);
        
        String objectKey = "tickets/" + ticketId + ".pdf";
        uploadToMinio(objectKey, pdfBytes);
        
        ticket.setPdfUrl(objectKey);
        ticketRepository.save(ticket);
        
        return pdfBytes;
    }

    private void uploadToMinio(String key, byte[] content) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType("application/pdf")
                    .build());
            log.info("Uploaded to MinIO: {}", key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload PDF to MinIO", e);
        }
    }

    private byte[] downloadFromMinio(String key) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .build());
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download PDF from MinIO", e);
        }
    }
}