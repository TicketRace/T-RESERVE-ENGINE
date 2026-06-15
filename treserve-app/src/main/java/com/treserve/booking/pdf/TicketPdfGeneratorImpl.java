package com.treserve.booking.pdf;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.pdf.PdfGenerator;
import com.treserve.ticket.service.TicketPdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketPdfGeneratorImpl implements PdfGenerator {

    private final TicketPdfGenerator pdfGenerator;

    @Override
    public byte[] generatePdf(Ticket ticket) {
        return pdfGenerator.generatePdf(ticket);
    }
}