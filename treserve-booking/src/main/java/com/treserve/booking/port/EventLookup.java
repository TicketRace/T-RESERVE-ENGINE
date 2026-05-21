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
}
