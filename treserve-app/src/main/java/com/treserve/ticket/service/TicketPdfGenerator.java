package com.treserve.ticket.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.treserve.booking.entity.Ticket;
import com.treserve.event.entity.Event;
import com.treserve.event.repository.EventRepository;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import com.treserve.venue.entity.Seat;
import com.treserve.venue.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketPdfGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final QrCodeGenerator qrCodeGenerator;

    public byte[] generatePdf(Ticket ticket) {
        Event event = eventRepository.findById(ticket.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found: " + ticket.getEventId()));
        
        Seat seat = seatRepository.findById(ticket.getSeatId())
                .orElseThrow(() -> new RuntimeException("Seat not found: " + ticket.getSeatId()));
        
        User user = userRepository.findById(ticket.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + ticket.getUserId()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // русские шрифты через стандартный HELVETICA
            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD);
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

            // Заголовок
            Paragraph title = new Paragraph("ЭЛЕКТРОННЫЙ БИЛЕТ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Название мероприятия
            Paragraph eventTitle = new Paragraph(event.getTitle(), sectionFont);
            eventTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(eventTitle);
            document.add(new Paragraph("\n"));

            // Таблица с дизайном
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);

            addStyledTableCell(table, "Мероприятие:", event.getTitle(), boldFont, normalFont);
            addStyledTableCell(table, "Дата и время:", DATE_FORMATTER.format(event.getStartTime().atZone(ZoneId.systemDefault())), boldFont, normalFont);
            addStyledTableCell(table, "Место проведения:", event.getVenue().getName(), boldFont, normalFont);
            addStyledTableCell(table, "Ряд:", seat.getRowLabel(), boldFont, normalFont);
            addStyledTableCell(table, "Место:", String.valueOf(seat.getSeatNumber()), boldFont, normalFont);
            addStyledTableCell(table, "Полное место:", seat.getSeatLabel(), boldFont, normalFont);
            addStyledTableCell(table, "Посетитель:", user.getName(), boldFont, normalFont);
            addStyledTableCell(table, "Email:", user.getEmail(), boldFont, normalFont);
            addStyledTableCell(table, "Цена:", String.format("%.2f ₽", ticket.getPrice()), boldFont, normalFont);
            addStyledTableCell(table, "ID билета:", String.valueOf(ticket.getId()), boldFont, normalFont);

            document.add(table);
            document.add(new Paragraph("\n"));

            // QR-код
            if (ticket.getVerifyToken() != null) {
                try {
                    byte[] qrCodeBytes = qrCodeGenerator.generateQrCode(ticket.getVerifyToken());
                    Image qrImage = Image.getInstance(qrCodeBytes);
                    qrImage.scaleToFit(150, 150);
                    qrImage.setAlignment(Element.ALIGN_CENTER);
                    document.add(qrImage);
                    
                    Font smallFont = new Font(Font.HELVETICA, 8, Font.ITALIC);
                    Paragraph qrHint = new Paragraph("Отсканируйте QR-код для проверки билета", smallFont);
                    qrHint.setAlignment(Element.ALIGN_CENTER);
                    document.add(qrHint);
                } catch (Exception e) {
                    log.error("Failed to generate QR code for ticket {}", ticket.getId(), e);
                    Font warningFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
                    Paragraph qrFallback = new Paragraph("Токен для проверки: " + ticket.getVerifyToken(), warningFont);
                    qrFallback.setAlignment(Element.ALIGN_CENTER);
                    document.add(qrFallback);
                }
            }

            document.close();

        } catch (Exception e) {
            log.error("Failed to generate PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return out.toByteArray();
    }

    private void addStyledTableCell(PdfPTable table, String label, String value, Font boldFont, Font normalFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, boldFont));
        labelCell.setGrayFill(0.95f);  // светло-серый фон
        labelCell.setBorder(Rectangle.BOX);
        labelCell.setPadding(6);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorder(Rectangle.BOX);
        valueCell.setPadding(6);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}