package com.treserve.ticket.service;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketDownloadServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketPdfGenerator pdfGenerator;

    @Mock
    private FileStorageService storageService;

    @Test
    @DisplayName("getOrCreateTicketPdf: если pdfUrl уже есть, скачивает PDF без генерации")
    void getOrCreateTicketPdf_whenPdfAlreadyExists_downloadsFromStorage() {
        TicketDownloadService service = service();
        byte[] storedPdf = new byte[] {1, 2, 3};
        Ticket ticket = bookedTicket(10L, "tickets/10.pdf");
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(storageService.downloadFile("tickets/10.pdf")).thenReturn(new ByteArrayInputStream(storedPdf));

        byte[] result = service.getOrCreateTicketPdf(10L, 99L);

        assertThat(result).containsExactly(storedPdf);
        verify(pdfGenerator, never()).generatePdf(ticket);
        verify(storageService, never()).uploadFile("tickets/10.pdf", storedPdf, "application/pdf");
        verify(ticketRepository, never()).updatePdfUrlIfNull(10L, "tickets/10.pdf");
    }

    @Test
    @DisplayName("getOrCreateTicketPdf: если скачивание существующего PDF упало, регенерирует и сохраняет новый")
    void getOrCreateTicketPdf_whenExistingPdfDownloadFails_regeneratesAndStoresNewPdf() {
        TicketDownloadService service = service();
        byte[] generatedPdf = new byte[] {4, 5, 6};
        Ticket ticket = bookedTicket(10L, "tickets/broken.pdf");
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
        when(storageService.downloadFile("tickets/broken.pdf")).thenThrow(new RuntimeException("storage down"));
        when(pdfGenerator.generatePdf(ticket)).thenReturn(generatedPdf);
        when(ticketRepository.updatePdfUrlIfNull(10L, "tickets/10.pdf")).thenReturn(1);

        byte[] result = service.getOrCreateTicketPdf(10L, 99L);

        assertThat(result).containsExactly(generatedPdf);
        verify(storageService).uploadFile("tickets/10.pdf", generatedPdf, "application/pdf");
        verify(ticketRepository).updatePdfUrlIfNull(10L, "tickets/10.pdf");
    }

    @Test
    @DisplayName("getOrCreateTicketPdf: при гонке скачивает PDF, который успел сохранить другой поток")
    void getOrCreateTicketPdf_whenAnotherThreadSavedPdf_downloadsRefreshedPdf() {
        TicketDownloadService service = service();
        byte[] generatedPdf = new byte[] {7, 8, 9};
        byte[] winnerPdf = new byte[] {9, 8, 7};
        Ticket initialTicket = bookedTicket(10L, null);
        Ticket refreshedTicket = bookedTicket(10L, "tickets/winner.pdf");
        when(ticketRepository.findById(10L))
                .thenReturn(Optional.of(initialTicket))
                .thenReturn(Optional.of(refreshedTicket));
        when(pdfGenerator.generatePdf(initialTicket)).thenReturn(generatedPdf);
        when(ticketRepository.updatePdfUrlIfNull(10L, "tickets/10.pdf")).thenReturn(0);
        when(storageService.downloadFile("tickets/winner.pdf")).thenReturn(new ByteArrayInputStream(winnerPdf));

        byte[] result = service.getOrCreateTicketPdf(10L, 99L);

        assertThat(result).containsExactly(winnerPdf);
        verify(storageService).uploadFile("tickets/10.pdf", generatedPdf, "application/pdf");
        verify(storageService).downloadFile("tickets/winner.pdf");
    }

    @Test
    @DisplayName("getOrCreateTicketPdf: чужой билет нельзя скачать")
    void getOrCreateTicketPdf_whenTicketBelongsToAnotherUser_throwsIllegalArgument() {
        TicketDownloadService service = service();
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(bookedTicket(10L, null)));

        assertThatThrownBy(() -> service.getOrCreateTicketPdf(10L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ticket does not belong to user");

        verify(pdfGenerator, never()).generatePdf(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("getOrCreateTicketPdf: PDF доступен только для BOOKED билета")
    void getOrCreateTicketPdf_whenTicketNotBooked_throwsIllegalState() {
        TicketDownloadService service = service();
        Ticket ticket = bookedTicket(10L, null);
        ticket.setStatus(TicketStatus.LOCKED);
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getOrCreateTicketPdf(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ticket is not confirmed");

        verify(pdfGenerator, never()).generatePdf(ticket);
    }

    private TicketDownloadService service() {
        return new TicketDownloadService(ticketRepository, pdfGenerator, storageService);
    }

    private static Ticket bookedTicket(Long id, String pdfUrl) {
        return Ticket.builder()
                .id(id)
                .eventId(1L)
                .seatId(2L)
                .userId(99L)
                .status(TicketStatus.BOOKED)
                .price(new BigDecimal("1000.00"))
                .pdfUrl(pdfUrl)
                .build();
    }
}
