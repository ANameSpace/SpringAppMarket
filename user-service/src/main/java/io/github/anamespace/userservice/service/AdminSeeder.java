package io.github.anamespace.userservice.service;

import io.github.anamespace.userservice.entity.Role;
import io.github.anamespace.userservice.entity.User;
import io.github.anamespace.userservice.entity.UserStatus;
import io.github.anamespace.userservice.repository.RoleRepository;
import io.github.anamespace.userservice.repository.UserRepository;
import io.github.anamespace.userservice.util.AvatarColors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminSeeder(RoleRepository roleRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${APP_USER_NAME:admin}") String adminUsername,
                       @Value("${APP_USER_PASSWORD}") String adminPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Transactional
    public void seed() {
        Role adminRole = findOrCreateRole("ADMIN");
        Role userRole = findOrCreateRole("USER");
        findOrCreateRole("DEVELOPER");

        User admin = userRepository.findByUsername(adminUsername).orElseGet(User::new);

        if (admin.getId() == null) {
            admin.setUsername(adminUsername);
            admin.setEmail(adminUsername.toLowerCase(Locale.ROOT) + "@springappmarket.local");
            admin.setDisplayName(adminUsername);
            admin.setAvatarColor(AvatarColors.random());
        }
        if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            admin.setEmail(adminUsername.toLowerCase(Locale.ROOT) + "@springappmarket.local");
        }
        if (admin.getDisplayName() == null || admin.getDisplayName().isBlank()) {
            admin.setDisplayName(adminUsername);
        }
        if (admin.getAvatarColor() == null || admin.getAvatarColor().isBlank()) {
            admin.setAvatarColor(AvatarColors.random());
        }

        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEnabled(true);
        admin.setStatus(UserStatus.ACTIVE);
        admin.getRoles().clear();
        admin.getRoles().add(userRole);
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        log.info("Seeded admin user '{}' with roles ADMIN and USER", adminUsername);
    }

    private Role findOrCreateRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }
}
