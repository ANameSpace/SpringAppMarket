package io.github.anamespace.userservice.service;

import io.github.anamespace.userservice.dto.LoginRequest;
import io.github.anamespace.userservice.dto.RegisterRequest;
import io.github.anamespace.userservice.dto.UserResponse;
import io.github.anamespace.userservice.entity.Role;
import io.github.anamespace.userservice.entity.User;
import io.github.anamespace.userservice.entity.UserStatus;
import io.github.anamespace.userservice.exception.EmailAlreadyExistsException;
import io.github.anamespace.userservice.exception.UsernameAlreadyExistsException;
import io.github.anamespace.userservice.mapper.UserMapper;
import io.github.anamespace.userservice.repository.RoleRepository;
import io.github.anamespace.userservice.repository.UserRepository;
import io.github.anamespace.userservice.util.AvatarColors;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + authentication.getName()));

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        Role userRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Role " + DEFAULT_ROLE + " is not configured"));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(username);
        user.setAvatarColor(AvatarColors.random());
        user.setStatus(UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.getRoles().add(userRole);

        return userMapper.toResponse(userRepository.save(user));
    }
}
