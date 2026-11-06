package io.github.anamespace.userservice.config;

import io.github.anamespace.userservice.entity.Role;
import io.github.anamespace.userservice.repository.UserRepository;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserRepository userRepository;

    public JwtTokenCustomizer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        String username = null;
        if (context.getAuthorization() != null) {
            username = context.getAuthorization().getPrincipalName();
        } else if (context.getPrincipal() != null) {
            username = context.getPrincipal().getName();
        }

        if (username == null || username.isBlank()) {
            return;
        }

        userRepository.findByUsername(username).ifPresent(user -> {
            List<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .sorted()
                    .toList();
            context.getClaims().claim("uid", user.getId());
            context.getClaims().claim("preferred_username", user.getUsername());
            context.getClaims().claim("email", user.getEmail());
            context.getClaims().claim("roles", roles);
        });
    }
}
