package com.treserve.config;

import com.treserve.common.utils.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Slf4j
public class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3 минуты

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        boolean cookieFound = CookieUtils.getCookie(request, COOKIE_NAME).isPresent();
        log.info("[OAuth2] loadAuthorizationRequest: cookieFound={}, uri={}",
                cookieFound, request.getRequestURI());
        if (!cookieFound) {
            Cookie[] all = request.getCookies();
            log.warn("[OAuth2] Cookie '{}' NOT FOUND. All cookies: {}",
                    COOKIE_NAME,
                    all == null ? "null" : java.util.Arrays.stream(all)
                            .map(Cookie::getName).toList());
        }
        return CookieUtils.getCookie(request, COOKIE_NAME)
                .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");

        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response, COOKIE_NAME);
            return;
        }

        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        log.info("[OAuth2] saveAuthorizationRequest: isSecure={}, request.isSecure()={}, X-Forwarded-Proto={}, scheme={}",
                isSecure,
                request.isSecure(),
                request.getHeader("X-Forwarded-Proto"),
                request.getScheme());

        CookieUtils.addCookie(response, COOKIE_NAME,
                CookieUtils.serialize(authorizationRequest),
                COOKIE_EXPIRE_SECONDS,
                isSecure);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(response, "response cannot be null");
        OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            CookieUtils.deleteCookie(request, response, COOKIE_NAME);
        }
        return authorizationRequest;
    }
}
