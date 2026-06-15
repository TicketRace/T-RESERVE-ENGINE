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
     * Пишем Set-Cookie заголовок напрямую, а не через Cookie API.
     * Причина: Tomcat/Servlet Cookie API не гарантирует корректную сериализацию
     * SameSite=None в паре с Secure через cookie.setAttribute() во всех версиях.
     * Chrome ОТБРАСЫВАЕТ SameSite=None куки без Secure=true — это первопричина
     * authorization_request_not_found в Railway деплоях.
     *
     * @param secure true для HTTPS (прод/Railway), false для HTTP (локально)
     */
    public static void addCookie(HttpServletResponse response, String name, String value,
            int maxAge, boolean secure) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        sb.append("; Path=/");
        sb.append("; HttpOnly");
        sb.append("; Max-Age=").append(maxAge);
        if (secure) {
            sb.append("; Secure");
            sb.append("; SameSite=None"); // Требуется для cross-site OAuth2 callback (Google → backend)
        } else {
            sb.append("; SameSite=Lax");  // Локально HTTP — Lax достаточно
        }
        response.addHeader("Set-Cookie", sb.toString());
    }

    /**
     * Удаляет cookie установкой Max-Age=0.
     * Также пишем напрямую в заголовок — по той же причине что и addCookie.
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                // native strategy: request.isSecure() уже true на Railway (Tomcat RemoteIpValve)
                boolean isSecure = request.isSecure()
                        || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
                StringBuilder sb = new StringBuilder();
                sb.append(name).append("=");
                sb.append("; Path=/");
                sb.append("; HttpOnly");
                sb.append("; Max-Age=0");
                if (isSecure) {
                    sb.append("; Secure");
                    sb.append("; SameSite=None");
                } else {
                    sb.append("; SameSite=Lax");
                }
                response.addHeader("Set-Cookie", sb.toString());
            }
        }
    }

    @SuppressWarnings("deprecation") // SerializationUtils deprecated в Spring 6.x, но безопасно для OAuth2AuthorizationRequest в httpOnly cookie
    public static String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    @SuppressWarnings("deprecation")
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie.getValue());
        return cls.cast(SerializationUtils.deserialize(decodedBytes));
    }
}
