package com.treserve.ticket.service;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketDownloadService {

    private final TicketRepository ticketRepository;
    private final TicketPdfGenerator pdfGenerator;
    private final FileStorageService storageService;  // ← используем интерфейс

    @Transactional
    public byte[] getOrCreateTicketPdf(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        if (ticket.getUserId() == null || !ticket.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Ticket does not belong to user");
        }

        if (ticket.getStatus() != TicketStatus.BOOKED) {
            throw new IllegalStateException("Ticket is not confirmed (status: " + ticket.getStatus() + ")");
        }

        if (ticket.getPdfUrl() != null) {
            log.info("PDF already exists for ticket {}, downloading from storage", ticketId);
            try {
                return downloadFromStorage(ticket.getPdfUrl());
            } catch (Exception e) {
                log.error("Failed to download PDF from storage, will regenerate: {}", e.getMessage());
            }
        }

        log.info("Generating PDF for ticket {}", ticketId);
        byte[] pdfBytes = pdfGenerator.generatePdf(ticket);
        
        String objectKey = "tickets/" + ticketId + ".pdf";
        storageService.uploadFile(objectKey, pdfBytes, "application/pdf");
        
        int updated = ticketRepository.updatePdfUrlIfNull(ticketId, objectKey);
        
        if (updated == 0) {
            log.info("Another thread already saved PDF for ticket {}", ticketId);
            Ticket refreshedTicket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
            return downloadFromStorage(refreshedTicket.getPdfUrl());
        }
        
        return pdfBytes;
    }

    private byte[] downloadFromStorage(String key) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             InputStream inputStream = storageService.downloadFile(key)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download PDF from storage", e);
        }
    }
}