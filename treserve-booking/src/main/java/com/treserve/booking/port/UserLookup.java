package com.treserve.booking.port;

/**
 * Порт-интерфейс: модуль booking должен проверять существование пользователей,
 * но не может напрямую зависеть от JPA-репозитория модуля user.
 *
 * Реализацию предоставляет treserve-app (JpaUserLookup).
 * Этот интерфейс — API-граница, которая позволяет вынести booking в микросервис.
 */
public interface UserLookup {
    UserInfo findById(Long id);
    boolean existsById(Long id);

    record UserInfo(Long id, String email, String name) {}
}
