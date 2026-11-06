package io.github.anamespace.appservice.security;

import io.github.anamespace.appservice.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class CurrentUserService {

    public CurrentUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            return null;
        }

        Jwt jwt = token.getToken();
        Long userId = readUserId(jwt);
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) {
            username = jwt.getSubject();
        }

        Set<String> roles = new LinkedHashSet<>();
        List<String> claimRoles = jwt.getClaimAsStringList("roles");
        if (claimRoles != null) {
            roles.addAll(claimRoles);
        }

        return new CurrentUser(userId, username, Set.copyOf(roles));
    }

    public CurrentUser require() {
        CurrentUser user = get();
        if (user == null) {
            throw new UnauthorizedException("Требуется авторизация");
        }
        return user;
    }

    public Long requireId() {
        CurrentUser user = require();
        if (user.id() == null) {
            throw new UnauthorizedException("В токене отсутствует идентификатор пользователя");
        }
        return user.id();
    }

    private Long readUserId(Jwt jwt) {
        Object value = jwt.getClaim("uid");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
