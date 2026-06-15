package com.treserve.booking.port;

/**
 * Порт-интерфейс: модуль booking должен проверять существование events,
 * но не может напрямую зависеть от JPA-репозитория модуля event/app.
 *
 * Реализацию предоставляет treserve-app (JpaEventLookup).
 * Этот интерфейс — API-граница, которая позволяет вынести booking в микросервис.
 */
public interface EventLookup {
    boolean existsById(Long eventId);

    /**
     * Получает название мероприятия по ID.
     * 
     * @param eventId ID мероприятия
     * @return название мероприятия или "Unknown Event", если не найдено
     */
    String getEventTitle(Long eventId);
}
