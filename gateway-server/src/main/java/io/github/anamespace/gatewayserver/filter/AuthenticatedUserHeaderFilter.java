package io.github.anamespace.gatewayserver.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adds the authenticated gateway user, numeric user id and roles to the
 * downstream request. Incoming X-Authenticated-* headers are always discarded
 * so clients cannot spoof the identity that the frontend displays.
 */
@Component
public class AuthenticatedUserHeaderFilter extends OncePerRequestFilter {

    public static final String USER_HEADER = "X-Authenticated-User";
    public static final String USER_ID_HEADER = "X-Authenticated-User-Id";
    public static final String ROLES_HEADER = "X-Authenticated-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = null;
        String userId = null;
        String roles = null;

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            username = resolveUsername(authentication);
            userId = resolveUserId(authentication);
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring("ROLE_".length()))
                    .sorted()
                    .collect(Collectors.joining(","));
        }

        filterChain.doFilter(
                new AuthenticatedHeadersRequest(request, username, userId, roles),
                response);
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            Object preferredUsername = oidcUser.getClaims().get("preferred_username");
            if (preferredUsername instanceof String value && !value.isBlank()) {
                return value;
            }
        }
        if (authentication instanceof JwtAuthenticationToken token) {
            String preferredUsername = token.getToken().getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
        }
        return authentication.getName();
    }

    private String resolveUserId(Authentication authentication) {
        Object value = null;
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            value = oidcUser.getClaims().get("uid");
        } else if (authentication instanceof JwtAuthenticationToken token) {
            value = token.getToken().getClaim("uid");
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private static final class AuthenticatedHeadersRequest extends HttpServletRequestWrapper {

        private final Map<String, String> headers;

        private AuthenticatedHeadersRequest(HttpServletRequest request,
                                            String username,
                                            String userId,
                                            String roles) {
            super(request);

            Map<String, String> values = new LinkedHashMap<>();
            if (username != null && !username.isBlank()) {
                values.put(USER_HEADER, username);
            }
            if (userId != null && !userId.isBlank()) {
                values.put(USER_ID_HEADER, userId);
            }
            if (roles != null && !roles.isBlank()) {
                values.put(ROLES_HEADER, roles);
            }
            this.headers = Collections.unmodifiableMap(values);
        }

        @Override
        public String getHeader(String name) {
            if (isManaged(name)) {
                return headers.get(canonicalName(name));
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isManaged(name)) {
                String value = headers.get(canonicalName(name));
                return value == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> originalNames = super.getHeaderNames();

            while (originalNames != null && originalNames.hasMoreElements()) {
                String name = originalNames.nextElement();
                if (!isManaged(name)) {
                    names.add(name);
                }
            }

            names.addAll(headers.keySet());
            return Collections.enumeration(names);
        }

        private static boolean isManaged(String name) {
            return canonicalName(name) != null;
        }

        private static String canonicalName(String name) {
            if (USER_HEADER.equalsIgnoreCase(name)) {
                return USER_HEADER;
            }
            if (USER_ID_HEADER.equalsIgnoreCase(name)) {
                return USER_ID_HEADER;
            }
            if (ROLES_HEADER.equalsIgnoreCase(name)) {
                return ROLES_HEADER;
            }
            return null;
        }
    }
}
