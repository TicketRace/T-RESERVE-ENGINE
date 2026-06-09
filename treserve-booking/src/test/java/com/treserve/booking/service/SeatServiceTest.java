package com.treserve.booking.service;

import com.treserve.booking.dto.SeatInfo;
import com.treserve.booking.port.EventLookup;
import com.treserve.booking.repository.SeatInfoRow;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventLookup eventLookup;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("getSeats: проверяет существование события и маппит проекции мест")
    void getSeats_mapsSeatInfoRows() {
        SeatService service = new SeatService(ticketRepository, eventLookup, messagingTemplate);
        when(eventLookup.existsById(7L)).thenReturn(true);
        when(ticketRepository.findSeatsByEventId(7L)).thenReturn(List.of(
                row(101L, "A-1", "A", 1, "AVAILABLE", new BigDecimal("1000.00")),
                row(102L, "A-2", "A", 2, "BOOKED", new BigDecimal("1200.00"))
        ));

        List<SeatInfo> seats = service.getSeats(7L);

        assertThat(seats).hasSize(2);
        assertThat(seats.get(0)).usingRecursiveComparison().isEqualTo(
                new SeatInfo(101L, "A-1", "A", 1, "AVAILABLE", new BigDecimal("1000.00"))
        );
        assertThat(seats.get(1)).usingRecursiveComparison().isEqualTo(
                new SeatInfo(102L, "A-2", "A", 2, "BOOKED", new BigDecimal("1200.00"))
        );
    }

    @Test
    @DisplayName("getSeats: неизвестное событие → ResourceNotFoundException без запроса мест")
    void getSeats_whenEventDoesNotExist_throwsResourceNotFound() {
        SeatService service = new SeatService(ticketRepository, eventLookup, messagingTemplate);
        when(eventLookup.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.getSeats(404L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).findSeatsByEventId(404L);
    }

    private static SeatInfoRow row(Long seatId,
                                   String seatLabel,
                                   String rowLabel,
                                   Integer seatNumber,
                                   String status,
                                   BigDecimal price) {
        return new SeatInfoRow() {
            @Override
            public Long getSeatId() {
                return seatId;
            }

            @Override
            public String getSeatLabel() {
                return seatLabel;
            }

            @Override
            public String getRowLabel() {
                return rowLabel;
            }

            @Override
            public Integer getSeatNumber() {
                return seatNumber;
            }

            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public BigDecimal getPrice() {
                return price;
            }
        };
    }
}
