package io.github.anamespace.userservice.config;

import io.github.anamespace.userservice.entity.Role;
import io.github.anamespace.userservice.repository.UserRepository;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class OidcUserInfoMapper implements Function<OidcUserInfoAuthenticationContext, OidcUserInfo> {

    private final UserRepository userRepository;

    public OidcUserInfoMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUserInfo apply(OidcUserInfoAuthenticationContext context) {
        String username = context.getAuthorization().getPrincipalName();

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", username);
        claims.put("preferred_username", username);

        userRepository.findByUsername(username).ifPresent(user -> {
            // app-service uses the numeric uid for developer_id and user_app_downloads.
            claims.put("uid", user.getId());
            // Grafana requires an email claim; without it, it falls back to the
            // non-standard /userinfo/emails endpoint, which this server does not expose.
            claims.put("email", user.getEmail());
            claims.put("roles", user.getRoles().stream()
                    .map(Role::getName)
                    .sorted()
                    .toList());
        });

        return new OidcUserInfo(claims);
    }
}
