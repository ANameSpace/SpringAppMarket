package io.github.anamespace.userservice.mapper;

import io.github.anamespace.userservice.dto.UserResponse;
import io.github.anamespace.userservice.entity.Role;
import io.github.anamespace.userservice.entity.User;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarColor(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
