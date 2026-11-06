package io.github.anamespace.appservice.security;

import io.github.anamespace.appservice.entity.App;

import java.util.Set;

public record CurrentUser(
        Long id,
        String username,
        Set<String> roles
) {

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }

    public boolean isDeveloper() {
        return roles.contains("DEVELOPER");
    }

    public boolean isOwner(App app) {
        return id != null && id.equals(app.getDeveloperId());
    }
}
