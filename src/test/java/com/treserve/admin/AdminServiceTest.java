package com.treserve.admin;

import com.treserve.booking.Ticket;
import com.treserve.booking.TicketRepository;
import com.treserve.booking.TicketStatus;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.event.Event;
import com.treserve.event.EventRepository;
import com.treserve.event.dto.EventCreateRequest;
import com.treserve.event.dto.EventResponse;
import com.treserve.event.dto.EventUpdateRequest;
import com.treserve.user.User;
import com.treserve.user.UserRepository;
import com.treserve.venue.Seat;
import com.treserve.venue.SeatRepository;
import com.treserve.venue.Venue;
import com.treserve.venue.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-04T10:00:00Z");

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminService adminService;

    private Venue testVenue;
    private User testAdmin;
    private EventCreateRequest createRequest;
    private EventUpdateRequest updateRequest;
    private List<Seat> testSeats;

    @BeforeEach
    void setUp() {
        testVenue = Venue.builder()
                .id(1L)
                .name("Тестовая площадка")
                .address("Москва, ул. Тестовая, 1")
                .rowsCount(3)
                .colsCount(5)
                .build();

        testAdmin = User.builder()
                .id(1L)
                .email("admin@test.com")
                .name("Admin")
                .role("ADMIN")
                .build();

        createRequest = new EventCreateRequest();
        createRequest.setTitle("Новый концерт");
        createRequest.setDescription("Описание нового концерта");
        createRequest.setVenueId(1L);
        createRequest.setStartTime(NOW.plusSeconds(7200));
        createRequest.setBasePrice(BigDecimal.valueOf(1500));
        createRequest.setImageUrl("https://example.com/image.jpg");
        createRequest.setAgeRestriction("16+");
        createRequest.setCategory("CONCERT");
        createRequest.setDurationMinutes(120);

        updateRequest = new EventUpdateRequest();
        updateRequest.setTitle("Обновлённый концерт");
        updateRequest.setDescription("Новое описание");
        updateRequest.setStatus("DRAFT");
        updateRequest.setBasePrice(BigDecimal.valueOf(2000));
        updateRequest.setStartTime(NOW.plusSeconds(10800));
        updateRequest.setImageUrl("https://example.com/new-image.jpg");
        updateRequest.setAgeRestriction("18+");
        updateRequest.setCategory("SPORT");
        updateRequest.setDurationMinutes(150);

        testSeats = new ArrayList<>();
        String[] rows = {"A", "B", "C"};
        for (String row : rows) {
            for (int num = 1; num <= 5; num++) {
                Seat seat = Seat.builder()
                        .id((long) (testSeats.size() + 1))
                        .venue(testVenue)
                        .rowLabel(row)
                        .seatNumber(num)
                        .build();
                testSeats.add(seat);
            }
        }
    }

    // ==================== createEvent ====================

    @Test
    @DisplayName("createEvent: успешное создание мероприятия с генерацией билетов (пакетная вставка)")
    void createEvent_success() {
        try (MockedStatic<Instant> instantMock = mockStatic(Instant.class)) {
            instantMock.when(Instant::now).thenReturn(NOW);

            when(venueRepository.findById(1L)).thenReturn(Optional.of(testVenue));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
            
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event event = invocation.getArgument(0);
                ReflectionTestUtils.setField(event, "id", 1L);
                return event;
            });
            
            when(seatRepository.findByVenueId(1L)).thenReturn(testSeats);
            when(ticketRepository.saveAll(anyIterable())).thenReturn(new ArrayList<>());

            EventResponse response = adminService.createEvent(createRequest, 1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("Новый концерт");
            assertThat(response.getDescription()).isEqualTo("Описание нового концерта");
            assertThat(response.getBasePrice()).isEqualByComparingTo("1500");
            assertThat(response.getVenueId()).isEqualTo(1L);
            assertThat(response.getVenueName()).isEqualTo("Тестовая площадка");
            assertThat(response.getStatus()).isEqualTo("ACTIVE");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<Ticket>> ticketsCaptor = ArgumentCaptor.forClass(Iterable.class);
            verify(ticketRepository, times(1)).saveAll(ticketsCaptor.capture());

            List<Ticket> tickets = StreamSupport.stream(
                    ticketsCaptor.getValue().spliterator(), false
            ).toList();

            assertThat(tickets).hasSize(testSeats.size());
            assertThat(tickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
                assertThat(ticket.getPrice()).isEqualByComparingTo(createRequest.getBasePrice());
                assertThat(ticket.getEvent().getId()).isEqualTo(1L);
                assertThat(ticket.getSeat()).isNotNull();
            });
        }
    }

    @Test
    @DisplayName("createEvent: площадка не найдена → ResourceNotFoundException")
    void createEvent_venueNotFound_throwsException() {
        when(venueRepository.findById(999L)).thenReturn(Optional.empty());
        createRequest.setVenueId(999L);

        assertThatThrownBy(() -> adminService.createEvent(createRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venue not found with id: 999");

        verify(eventRepository, never()).save(any());
        verify(ticketRepository, never()).saveAll(any());
        verify(userRepository, never()).findById(any());
        verify(seatRepository, never()).findByVenueId(any());
    }

    @Test
    @DisplayName("createEvent: администратор не найден → ResourceNotFoundException")
    void createEvent_adminNotFound_throwsException() {
        when(venueRepository.findById(1L)).thenReturn(Optional.of(testVenue));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createEvent(createRequest, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Admin not found with id: 999");

        verify(eventRepository, never()).save(any());
        verify(ticketRepository, never()).saveAll(any());
        verify(seatRepository, never()).findByVenueId(any());
    }

    // ==================== updateEvent ====================

    @Test
    @DisplayName("updateEvent: успешное обновление мероприятия до начала продаж")
    void updateEvent_success() {
    try (MockedStatic<Instant> instantMock = mockStatic(Instant.class)) {
        instantMock.when(Instant::now).thenReturn(NOW);

        // ПРАВИЛЬНОЕ создание Event с указанием ВСЕХ полей
        Event event = new Event();
        event.setId(1L);
        event.setTitle("Тестовый концерт");
        event.setDescription("Описание тестового концерта");
        event.setVenue(testVenue);
        event.setStartTime(NOW.plusSeconds(3600));
        event.setBasePrice(BigDecimal.valueOf(1000));
        event.setStatus("ACTIVE");
        event.setCreatedBy(testAdmin);

        System.out.println("=== ДИАГНОСТИКА ===");
        System.out.println("NOW = " + NOW);
        System.out.println("event.getStartTime() = " + event.getStartTime());

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        EventResponse response = adminService.updateEvent(1L, updateRequest, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(event.getTitle()).isEqualTo("Обновлённый концерт");
        assertThat(event.getDescription()).isEqualTo("Новое описание");
        assertThat(event.getStatus()).isEqualTo("DRAFT");
        assertThat(event.getBasePrice()).isEqualByComparingTo("2000");
        assertThat(event.getStartTime()).isEqualTo(NOW.plusSeconds(10800));
        assertThat(event.getImageUrl()).isEqualTo("https://example.com/new-image.jpg");
        assertThat(event.getAgeRestriction()).isEqualTo("18+");
        assertThat(event.getCategory()).isEqualTo("SPORT");
        assertThat(event.getDurationMinutes()).isEqualTo(150);

        verify(eventRepository).save(event);
    }
}
    @Test
    @DisplayName("updateEvent: обновление только части полей (null не перезаписывает существующие)")
    void updateEvent_nullOptionalFields_doesNotOverwriteExistingValues() {
    try (MockedStatic<Instant> instantMock = mockStatic(Instant.class)) {
        instantMock.when(Instant::now).thenReturn(NOW);

        Event event = new Event();
        event.setId(1L);
        event.setTitle("Тестовый концерт");
        event.setDescription("Описание тестового концерта");
        event.setVenue(testVenue);
        event.setStartTime(NOW.plusSeconds(3600));
        event.setBasePrice(BigDecimal.valueOf(1000));
        event.setStatus("ACTIVE");
        event.setCreatedBy(testAdmin);

        EventUpdateRequest partialRequest = new EventUpdateRequest();
        partialRequest.setTitle("Только заголовок обновлён");
        partialRequest.setStatus("ACTIVE");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        adminService.updateEvent(1L, partialRequest, 1L);

        assertThat(event.getTitle()).isEqualTo("Только заголовок обновлён");
        assertThat(event.getDescription()).isEqualTo("Описание тестового концерта");
        assertThat(event.getBasePrice()).isEqualByComparingTo("1000");
        assertThat(event.getStartTime()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(event.getDurationMinutes()).isEqualTo(120);
    }
}

    @Test
    @DisplayName("updateEvent: событие не найдено → ResourceNotFoundException")
    void updateEvent_eventNotFound_throwsException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateEvent(999L, updateRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found with id: 999");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateEvent: продажи уже начались → IllegalArgumentException")
    void updateEvent_afterSalesStarted_throwsException() {
    try (MockedStatic<Instant> instantMock = mockStatic(Instant.class)) {
        instantMock.when(Instant::now).thenReturn(NOW);

        Event pastEvent = new Event();
        pastEvent.setId(1L);
        pastEvent.setTitle("Тестовый концерт");
        pastEvent.setDescription("Описание тестового концерта");
        pastEvent.setVenue(testVenue);
        pastEvent.setStartTime(NOW.minusSeconds(3600));
        pastEvent.setBasePrice(BigDecimal.valueOf(1000));
        pastEvent.setStatus("ACTIVE");
        pastEvent.setCreatedBy(testAdmin);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(pastEvent));

        assertThatThrownBy(() -> adminService.updateEvent(1L, updateRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot edit event after sales started");

        verify(eventRepository, never()).save(any());
    }
}

    // ==================== deleteEvent ====================

    @Test
    @DisplayName("deleteEvent: успешное удаление мероприятия (нет оплаченных билетов)")
    void deleteEvent_noBookedTickets_success() {
        Event event = Event.builder()
                .id(1L)
                .title("Тестовый концерт")
                .description("Описание тестового концерта")
                .venue(testVenue)
                .startTime(NOW.plusSeconds(3600))
                .basePrice(BigDecimal.valueOf(1000))
                .status("ACTIVE")
                .createdBy(testAdmin)
                .build();

        List<Ticket> emptyTickets = new ArrayList<>();
        
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.hasBookedTickets(1L)).thenReturn(false);
        when(ticketRepository.findByEventIdWithSeat(1L)).thenReturn(emptyTickets);

        adminService.deleteEvent(1L);

        verify(ticketRepository).deleteAll(emptyTickets);
        verify(eventRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteEvent: событие с оплаченными билетами → IllegalArgumentException")
    void deleteEvent_withBookedTickets_throwsException() {
        Event event = Event.builder()
                .id(1L)
                .title("Тестовый концерт")
                .description("Описание тестового концерта")
                .venue(testVenue)
                .startTime(NOW.plusSeconds(3600))
                .basePrice(BigDecimal.valueOf(1000))
                .status("ACTIVE")
                .createdBy(testAdmin)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.hasBookedTickets(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminService.deleteEvent(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete event with BOOKED tickets");

        verify(ticketRepository, never()).deleteAll(any());
        verify(eventRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteEvent: событие не найдено → ResourceNotFoundException")
    void deleteEvent_eventNotFound_throwsException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteEvent(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found with id: 999");

        verify(ticketRepository, never()).deleteAll(any());
        verify(eventRepository, never()).deleteById(any());
    }
}