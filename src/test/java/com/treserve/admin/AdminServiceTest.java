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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для AdminService.
 *
 * Тестируем только бизнес-логику сервиса, все внешние зависимости замоканы.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

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
    private Event testEvent;
    private EventCreateRequest createRequest;
    private EventUpdateRequest updateRequest;
    private List<Seat> testSeats;

    @BeforeEach
    void setUp() {
        // Создаём тестовую площадку
        testVenue = Venue.builder()
                .id(1L)
                .name("Тестовая площадка")
                .address("Москва, ул. Тестовая, 1")
                .rowsCount(3)
                .colsCount(5)
                .build();

        // Создаём тестового администратора
        testAdmin = User.builder()
                .id(1L)
                .email("admin@test.com")
                .name("Admin")
                .role("ADMIN")
                .build();

        // Создаём тестовое событие
        testEvent = Event.builder()
                .id(1L)
                .title("Тестовый концерт")
                .description("Описание тестового концерта")
                .venue(testVenue)
                .startTime(Instant.now().plusSeconds(3600)) // через час
                .basePrice(BigDecimal.valueOf(1000))
                .status("ACTIVE")
                .createdBy(testAdmin)
                .build();

        // Создаём тестовый запрос на создание
        createRequest = new EventCreateRequest();
        createRequest.setTitle("Новый концерт");
        createRequest.setDescription("Описание нового концерта");
        createRequest.setVenueId(1L);
        createRequest.setStartTime(Instant.now().plusSeconds(7200));
        createRequest.setBasePrice(BigDecimal.valueOf(1500));
        createRequest.setImageUrl("https://example.com/image.jpg");
        createRequest.setAgeRestriction("16+");
        createRequest.setCategory("CONCERT");
        createRequest.setDurationMinutes(120);

        // Создаём тестовый запрос на обновление
        updateRequest = new EventUpdateRequest();
        updateRequest.setTitle("Обновлённый концерт");
        updateRequest.setDescription("Новое описание");
        updateRequest.setStatus("ACTIVE");
        updateRequest.setBasePrice(BigDecimal.valueOf(2000));

        // Создаём тестовые места (3 ряда × 5 мест = 15 мест)
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
        // given
        when(venueRepository.findById(1L)).thenReturn(Optional.of(testVenue));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testAdmin));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
        when(seatRepository.findByVenueId(1L)).thenReturn(testSeats);
        when(ticketRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

        // when
        EventResponse response = adminService.createEvent(createRequest, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Тестовый концерт");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        // Проверяем, что билеты сохраняются пакетно (один вызов saveAll)
        verify(ticketRepository, times(1)).saveAll(anyList());
        
        // Проверяем, что количество создаваемых билетов равно количеству мест
        verify(ticketRepository).saveAll(argThat(tickets -> 
            ((List<?>) tickets).size() == testSeats.size()
        ));
    }

    @Test
    @DisplayName("createEvent: площадка не найдена → ResourceNotFoundException (404)")
    void createEvent_venueNotFound_throws404() {
        // given
        when(venueRepository.findById(999L)).thenReturn(Optional.empty());
        createRequest.setVenueId(999L);

        // when/then
        assertThatThrownBy(() -> adminService.createEvent(createRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venue not found with id: 999");

        // Проверяем, что ничего не сохранялось
        verify(eventRepository, never()).save(any());
        verify(ticketRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("createEvent: администратор не найден → ResourceNotFoundException (404)")
    void createEvent_adminNotFound_throws404() {
        // given
        when(venueRepository.findById(1L)).thenReturn(Optional.of(testVenue));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> adminService.createEvent(createRequest, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Admin not found with id: 999");

        // Проверяем, что событие не создавалось
        verify(eventRepository, never()).save(any());
        verify(ticketRepository, never()).saveAll(any());
    }

    // ==================== updateEvent ====================

    @Test
    @DisplayName("updateEvent: успешное обновление мероприятия до начала продаж")
    void updateEvent_success() {
        // given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // when
        EventResponse response = adminService.updateEvent(1L, updateRequest, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        
        // Проверяем, что поля обновились
        assertThat(testEvent.getTitle()).isEqualTo("Обновлённый концерт");
        assertThat(testEvent.getDescription()).isEqualTo("Новое описание");
        assertThat(testEvent.getBasePrice()).isEqualTo(BigDecimal.valueOf(2000));
        
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("updateEvent: событие не найдено → ResourceNotFoundException (404)")
    void updateEvent_eventNotFound_throws404() {
        // given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> adminService.updateEvent(999L, updateRequest, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found with id: 999");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateEvent: продажи уже начались → IllegalArgumentException (400)")
    void updateEvent_afterSalesStarted_throws400() {
        // given
        // Устанавливаем время начала в прошлом (продажи уже начались)
        testEvent.setStartTime(Instant.now().minusSeconds(3600));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // when/then
        assertThatThrownBy(() -> adminService.updateEvent(1L, updateRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot edit event after sales started");

        verify(eventRepository, never()).save(any());
    }

    // ==================== deleteEvent ====================

    @Test
    @DisplayName("deleteEvent: успешное удаление мероприятия (нет оплаченных билетов)")
    void deleteEvent_noBookedTickets_success() {
        // given
        List<Ticket> emptyTickets = new ArrayList<>();
        
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.hasBookedTickets(1L)).thenReturn(false);
        when(ticketRepository.findByEventIdWithSeat(1L)).thenReturn(emptyTickets);
        doNothing().when(ticketRepository).deleteAll(emptyTickets);
        doNothing().when(eventRepository).deleteById(1L);

        // when
        adminService.deleteEvent(1L);

        // then
        verify(ticketRepository).deleteAll(emptyTickets);
        verify(eventRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteEvent: событие с оплаченными билетами → IllegalArgumentException (409)")
    void deleteEvent_withBookedTickets_throws400() {
        // given
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.hasBookedTickets(1L)).thenReturn(true);

        // when/then
        assertThatThrownBy(() -> adminService.deleteEvent(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete event with BOOKED tickets");

        // Проверяем, что удаление не произошло
        verify(ticketRepository, never()).deleteAll(any());
        verify(eventRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteEvent: событие не найдено → ResourceNotFoundException (404)")
    void deleteEvent_eventNotFound_throws404() {
        // given
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> adminService.deleteEvent(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found with id: 999");

        verify(ticketRepository, never()).deleteAll(any());
        verify(eventRepository, never()).deleteById(any());
    }
}