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
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class TicketPdfGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public byte[] generatePdf(Ticket ticket) {
        // Загружаем данные по ID
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

            // Заголовок
            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD);
            Paragraph title = new Paragraph("ЭЛЕКТРОННЫЙ БИЛЕТ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Название мероприятия
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Paragraph eventTitle = new Paragraph(event.getTitle(), sectionFont);
            eventTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(eventTitle);
            document.add(new Paragraph("\n"));

            // Таблица с деталями
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);

            addTableCell(table, "Мероприятие:", event.getTitle());
            addTableCell(table, "Дата и время:", DATE_FORMATTER.format(event.getStartTime()));
            addTableCell(table, "Место проведения:", event.getVenue().getName());
            addTableCell(table, "Ряд:", seat.getRowLabel());
            addTableCell(table, "Место:", String.valueOf(seat.getSeatNumber()));
            addTableCell(table, "Полное место:", seat.getSeatLabel());
            addTableCell(table, "Посетитель:", user.getName());
            addTableCell(table, "Email:", user.getEmail());
            addTableCell(table, "Цена:", String.format("%.2f ₽", ticket.getPrice()));
            addTableCell(table, "ID билета:", String.valueOf(ticket.getId()));

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return out.toByteArray();
    }

    private void addTableCell(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font valueFont = new Font(Font.HELVETICA, 12, Font.NORMAL);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}