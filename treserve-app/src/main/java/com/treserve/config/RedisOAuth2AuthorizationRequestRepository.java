package com.treserve.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Хранит OAuth2 Authorization Request в Redis вместо browser cookie.
 *
 * Почему не cookie:
 *   - up.railway.app входит в Public Suffix List браузеров.
 *   - frontend-xxx.up.railway.app и backend-yyy.up.railway.app — разные "сайты".
 *   - Chrome блокирует cross-site куки даже с SameSite=None; Secure ("third-party cookie
 *     blocking / tracking protection"), что вызывает authorization_request_not_found.
 *
 * Почему Redis:
 *   - State передаётся через URL параметр ?state=..., не через cookie.
 *   - Браузер в хранении не участвует — никаких cookie-блокировок.
 *   - Работает с любым кол-вом инстансов (shared state).
 *   - TTL = 5 минут (достаточно для авторизации через Google).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String KEY_PREFIX = "oauth2:state:";
    private static final long   TTL_SECONDS = 300; // 5 минут — достаточно для Google auth

    private final StringRedisTemplate redisTemplate;

    // ─── Save ──────────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("deprecation")
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            // Spring вызывает save(null) для очистки — удаляем по state из текущего запроса
            removeByState(request.getParameter("state"));
            return;
        }

        String state     = authorizationRequest.getState();
        String redisKey  = KEY_PREFIX + state;
        byte[] bytes     = SerializationUtils.serialize(authorizationRequest);
        String serialized = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        redisTemplate.opsForValue().set(redisKey, serialized, TTL_SECONDS, TimeUnit.SECONDS);
        log.info("[OAuth2] Saved in Redis: key={}", redisKey);
    }

    // ─── Load ──────────────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("deprecation")
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) {
            log.debug("[OAuth2] loadAuthorizationRequest: no state param in request");
            return null;
        }

        String redisKey   = KEY_PREFIX + state;
        String serialized = redisTemplate.opsForValue().get(redisKey);

        if (serialized == null) {
            log.warn("[OAuth2] loadAuthorizationRequest: key NOT FOUND in Redis: {}", redisKey);
            return null;
        }

        try {
            byte[] bytes = Base64.getUrlDecoder().decode(serialized);
            OAuth2AuthorizationRequest req =
                    (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
            log.info("[OAuth2] loadAuthorizationRequest: found in Redis for state={}", state);
            return req;
        } catch (Exception e) {
            log.error("[OAuth2] loadAuthorizationRequest: deserialization failed for state={}",
                    state, e);
            return null;
        }
    }

    // ─── Remove ────────────────────────────────────────────────────────────────

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        if (authRequest != null) {
            removeByState(authRequest.getState());
        }
        return authRequest;
    }

    // ─── Internal ──────────────────────────────────────────────────────────────

    private void removeByState(String state) {
        if (state != null) {
            Boolean deleted = redisTemplate.delete(KEY_PREFIX + state);
            log.info("[OAuth2] Removed from Redis: key={}, deleted={}", KEY_PREFIX + state, deleted);
        }
    }
}
