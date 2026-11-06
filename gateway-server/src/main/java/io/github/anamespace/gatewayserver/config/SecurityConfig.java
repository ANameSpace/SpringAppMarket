package io.github.anamespace.gatewayserver.config;

import io.github.anamespace.gatewayserver.filter.AuthenticatedUserHeaderFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.springframework.http.HttpMethod.GET;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
            AuthenticatedUserHeaderFilter authenticatedUserHeaderFilter,
            LogoutSuccessHandler oidcLogoutSuccessHandler,
            RequestCache requestCache) {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // The frontend is public: anonymous visitors see a login button.
                        // Only the API, docs and Grafana require a gateway session.
                        .requestMatchers("/", "/_next/**", "/static/**", "/assets/**",
                                "/favicon.ico", "/*.svg", "/sw.js", "/manifest.json",
                                "/robots.txt", "/sitemap.xml",
                                "/catalog", "/catalog/**", "/apps/**", "/developer", "/developer/**")
                        .permitAll()
                        // Public catalog reads. When a session exists, TokenRelay still
                        // forwards the bearer token and app-service can compute downloadState.
                        .requestMatchers(HttpMethod.GET, "/api/v1/apps/**", "/api/v1/categories/**")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/prometheus", "/healthz").permitAll()
                        // The gateway is the only public Swagger UI and spec aggregator.
                        // Only administrators may see it.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**", "/openapi/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/grafana/**").hasRole("ADMIN")
                        // Browser-facing authorization-server endpoints are proxied by the gateway.
                        .requestMatchers("/auth-server/**").permitAll()
                        .requestMatchers("/login/**", "/oauth2/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // Everything else is public. Unknown paths must not start an
                        // OAuth2 flow: otherwise a stray URL is saved as the post-login
                        // target and the user is redirected back to a 404 page.
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService)))
                // Spring Security appends the request-cache marker ("continue") to the
                // saved URL by default. For a browser-facing gateway that marker leaks
                // into the post-login redirect (e.g. /api/v1/users/me?continue), so
                // use a request cache without a matching parameter.
                .requestCache(cache -> cache.requestCache(requestCache))
                // The gateway is a browser-facing application: unauthenticated
                // requests start the OAuth2 login flow instead of returning 401.
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint(
                                        "/oauth2/authorization/gateway-client-oidc"),
                                AnyRequestMatcher.INSTANCE))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("GATEWAY_SESSION"))
                .addFilterAfter(authenticatedUserHeaderFilter, AuthorizationFilter.class)
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Saves protected requests so that login returns the user to the page they
     * originally asked for. The default matching parameter is disabled: it would
     * otherwise be appended to the redirect target (for example
     * {@code /api/v1/users/me?continue}).
     */
    /**
     * Grafana builds its OAuth {@code state} as base64 and does not percent-encode
     * the trailing {@code =} padding. The default strict firewall rejects such a
     * query parameter value, which turned the Grafana callback into a 500. Allow
     * {@code =} inside parameter values while keeping the remaining checks.
     */
    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        Predicate<String> allowedValues = StrictHttpFirewall.ALLOWED_PARAMETER_VALUES;
        firewall.setAllowedParameterValues(value -> allowedValues.test(value.replace("=", "")));
        return firewall;
    }

    @Bean
    public RequestCache requestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName(null);
        return requestCache;
    }

    /**
     * Ends the gateway session and then the authorization-server session at
     * user-service. Without the second step the user-service session would
     * still be active and the next login would silently reuse it.
     */
    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            String homeUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/")
                    .toUriString();

            if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
                String logoutUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/auth-server/connect/logout")
                        .queryParam("id_token_hint", oidcUser.getIdToken().getTokenValue())
                        .queryParam("post_logout_redirect_uri", homeUrl)
                        .build()
                        .encode()
                        .toUriString();
                response.sendRedirect(logoutUrl);
                return;
            }

            response.sendRedirect(homeUrl);
        };
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();

        return userRequest -> {
            OidcUser user = delegate.loadUser(userRequest);
            Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());

            List<String> roles = user.getClaimAsStringList("roles");
            if (roles != null) {
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
            }

            return new DefaultOidcUser(
                    authorities,
                    user.getIdToken(),
                    user.getUserInfo());
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
