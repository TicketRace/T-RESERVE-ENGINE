package com.treserve.common.utils;

import jakarta.servlet.http.Cookie; // still needed for getCookie() and deserialize()
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Добавляет cookie в ответ.
     *
     * Используем Cookie API (Томкэт автоматически экранирует спецсимволы в значении).
     * SameSite=None требует Secure=true — иначе Chrome выбрасывает куки.
     * Secure=true обеспечивается через native forward-headers-strategy
     * (Томкэт RemoteIpValve обрабатывает X-Forwarded-Proto).
     *
     * @param secure true для HTTPS (прод/Railway), false для HTTP (локально)
     */
    public static void addCookie(HttpServletResponse response, String name, String value,
            int maxAge, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(maxAge);
        // Jakarta Servlet 6.0 (Tomcat 10.1) поддерживает setAttribute() для SameSite
        cookie.setAttribute("SameSite", secure ? "None" : "Lax");
        response.addCookie(cookie);
    }

    /**
     * Удаляет cookie установкой Max-Age=0.
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                boolean isSecure = request.isSecure()
                        || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
                cookie.setValue("");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                cookie.setSecure(isSecure);
                cookie.setAttribute("SameSite", isSecure ? "None" : "Lax");
                response.addCookie(cookie);
            }
        }
    }

    /**
     * Сериализует объект в Base64 URL-безопасную строку.
     * Используем withoutPadding() — символ '=' не попадает в значение cookie
     * и не путает RFC 6265 парсер Tomcat.
     */
    @SuppressWarnings("deprecation")
    public static String serialize(Object object) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(SerializationUtils.serialize(object));
    }

    /**
     * Десериализует Base64 значение cookie.
     * Добавляет padding если нужно — обрабатывает оба варианта (с padding и без).
     */
    @SuppressWarnings("deprecation")
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        String value = cookie.getValue();
        // Нормализация padding для совместимости (вход: с '=' или без)
        int mod = value.length() % 4;
        if (mod == 2)      value += "==";
        else if (mod == 3) value += "=";
        byte[] decodedBytes = Base64.getUrlDecoder().decode(value);
        return cls.cast(SerializationUtils.deserialize(decodedBytes));
    }
}
