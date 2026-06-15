package com.treserve.booking.pdf;

import com.treserve.booking.entity.Ticket;

public interface PdfGenerator {
    byte[] generatePdf(Ticket ticket);
}