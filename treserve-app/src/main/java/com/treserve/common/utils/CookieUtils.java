package com.treserve.common.utils;

import jakarta.servlet.http.Cookie;
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
     * @param secure должен совпадать с request.isSecure() — true для HTTPS (прод),
     *               false для HTTP (локальная разработка)
     */
    public static void addCookie(HttpServletResponse response, String name, String value,
            int maxAge, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure); // HTTPS → true, localhost HTTP → false
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", secure ? "None" : "Lax"); // Important for cross-site Railway cookies
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
                    cookie.setSecure(isSecure);
                    cookie.setAttribute("SameSite", isSecure ? "None" : "Lax");
                    response.addCookie(cookie);
                }
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
