package io.github.anamespace.userservice.service;

import io.github.anamespace.userservice.dto.PageResponse;
import io.github.anamespace.userservice.dto.UpdateEmailRequest;
import io.github.anamespace.userservice.dto.UpdatePasswordRequest;
import io.github.anamespace.userservice.dto.UpdateProfileRequest;
import io.github.anamespace.userservice.dto.UserResponse;
import io.github.anamespace.userservice.entity.Role;
import io.github.anamespace.userservice.entity.User;
import io.github.anamespace.userservice.entity.UserStatus;
import io.github.anamespace.userservice.exception.EmailAlreadyExistsException;
import io.github.anamespace.userservice.exception.InvalidCurrentPasswordException;
import io.github.anamespace.userservice.exception.RoleConflictException;
import io.github.anamespace.userservice.exception.UserNotFoundException;
import io.github.anamespace.userservice.mapper.UserMapper;
import io.github.anamespace.userservice.repository.RoleRepository;
import io.github.anamespace.userservice.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_DEVELOPER = "DEVELOPER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Set<String> KNOWN_ROLES = Set.of(ROLE_USER, ROLE_DEVELOPER, ROLE_ADMIN);
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getProfile(String username) {
        return userMapper.toResponse(requireUser(username));
    }

    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = requireUser(username);

        if (request.displayName() != null) {
            String displayName = request.displayName().trim();
            if (displayName.isEmpty()) {
                throw new IllegalArgumentException("displayName must be between 1 and 100 characters");
            }
            user.setDisplayName(displayName);
        }

        if (request.avatarColor() != null) {
            user.setAvatarColor(request.avatarColor());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateEmail(String username, UpdateEmailRequest request) {
        User user = requireUser(username);
        String email = request.newEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, user.getId())) {
            throw new EmailAlreadyExistsException(email);
        }

        user.setEmail(email);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void updatePassword(String username, UpdatePasswordRequest request) {
        User user = requireUser(username);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse becomeDeveloper(String username) {
        User user = requireUser(username);

        boolean isAdmin = hasRole(user, ROLE_ADMIN);
        if (isAdmin) {
            throw new RoleConflictException("Administrators cannot become developers");
        }

        if (hasRole(user, ROLE_DEVELOPER)) {
            return userMapper.toResponse(user);
        }

        Role developerRole = roleRepository.findByName(ROLE_DEVELOPER)
                .orElseThrow(() -> new IllegalStateException("Role " + ROLE_DEVELOPER + " is not configured"));
        user.getRoles().add(developerRole);

        return userMapper.toResponse(userRepository.save(user));
    }

    public PageResponse<UserResponse> findUsers(int page, int size, String role, String status) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        String roleFilter = normalizeRoleFilter(role);
        UserStatus statusFilter = parseStatus(status);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (roleFilter != null) {
                Join<User, Role> roleJoin = root.join("roles", JoinType.INNER);
                predicates.add(criteriaBuilder.equal(roleJoin.get("name"), roleFilter));
                query.distinct(true);
            }

            if (statusFilter != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), statusFilter));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        Page<User> users = userRepository.findAll(specification, pageable);
        return PageResponse.from(users, userMapper::toResponse);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private boolean hasRole(User user, String role) {
        return user.getRoles().stream().anyMatch(candidate -> role.equals(candidate.getName()));
    }

    private String normalizeRoleFilter(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!KNOWN_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("Unknown role: " + role);
        }
        return normalized;
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown status: " + status);
        }
    }
}
