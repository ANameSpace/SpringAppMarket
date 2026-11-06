package io.github.anamespace.userservice.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http,
                                                             OidcUserInfoMapper oidcUserInfoMapper) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer.oidc(oidc -> oidc
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userInfoMapper(oidcUserInfoMapper)))
                )
                .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/signin"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/actuator/**").permitAll()
                        // Error rendering must stay public: an authorization request can
                        // fail before the user is authenticated, and the error page should
                        // never start another login round-trip.
                        .requestMatchers("/error").permitAll()
                        // Demo only: the gateway aggregates this OpenAPI document.
                        // Protect this endpoint again before using this stack outside a demo.
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        // Browser-facing sign-in, registration and their assets.
                        .requestMatchers("/signin", "/register", "/login", "/auth.css", "/css/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf((csrf) -> csrf.ignoringRequestMatchers("/api/v1/auth/login"))
                // The gateway relays the OAuth2 access token to the user-service API.
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .formLogin((form) -> form
                        .loginPage("/signin")
                        .loginProcessingUrl("/login")
                        // A relative redirect would lose the /auth-server context path
                        // in the browser, so build it from the current request.
                        .failureHandler((request, response, exception) ->
                                response.sendRedirect(request.getContextPath() + "/signin?error"))
                        .permitAll()
                );
        return http.build();
    }

    /**
     * The gateway builds its OAuth2 redirect URI from the host the browser used.
     * Both the canonical {@code localhost} name and the {@code 127.0.0.1} loopback
     * address must be registered, otherwise signing in from 127.0.0.1 fails after
     * the credentials are submitted with an invalid_redirect_uri error.
     */
    private static final String PUBLIC_BASE_URL = "http://localhost:8080";
    private static final String PUBLIC_LOOPBACK_URL = "http://127.0.0.1:8080";

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            @Value("${GATEWAY_CLIENT_SECRET}") String gatewayClientSecret,
            @Value("${GRAFANA_CLIENT_SECRET}") String grafanaClientSecret,
            PasswordEncoder passwordEncoder) {
        RegisteredClient gatewayClient = RegisteredClient.withId("gateway-id")
                .clientId("gateway-server")
                .clientSecret(passwordEncoder.encode(gatewayClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(PUBLIC_BASE_URL + "/login/oauth2/code/gateway-client-oidc")
                .redirectUri(PUBLIC_LOOPBACK_URL + "/login/oauth2/code/gateway-client-oidc")
                .postLogoutRedirectUri(PUBLIC_BASE_URL + "/")
                .postLogoutRedirectUri(PUBLIC_LOOPBACK_URL + "/")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireProofKey(false).build())
                .build();

        RegisteredClient grafanaClient = RegisteredClient.withId("grafana-id")
                .clientId("grafana")
                .clientSecret(passwordEncoder.encode(grafanaClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(PUBLIC_BASE_URL + "/grafana/login/generic_oauth")
                .redirectUri(PUBLIC_LOOPBACK_URL + "/grafana/login/generic_oauth")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder().requireProofKey(false).build())
                .build();

        return new InMemoryRegisteredClientRepository(gatewayClient, grafanaClient);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        JWKSet jwkSet = new JWKSet(Jwks.generateRsa());
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${AUTH_SERVER_ISSUER:http://localhost:8081/auth-server}") String issuer) {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }
}
