package io.github.anamespace.userservice.controller;

import io.github.anamespace.userservice.dto.PageResponse;
import io.github.anamespace.userservice.dto.UpdateEmailRequest;
import io.github.anamespace.userservice.dto.UpdatePasswordRequest;
import io.github.anamespace.userservice.dto.UpdateProfileRequest;
import io.github.anamespace.userservice.dto.UserResponse;
import io.github.anamespace.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    @PatchMapping("/me")
    public UserResponse updateMe(Authentication authentication,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authentication.getName(), request);
    }

    @PatchMapping("/me/email")
    public UserResponse updateEmail(Authentication authentication,
                                    @Valid @RequestBody UpdateEmailRequest request) {
        return userService.updateEmail(authentication.getName(), request);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(Authentication authentication,
                                               @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/become-developer")
    public UserResponse becomeDeveloper(Authentication authentication) {
        return userService.becomeDeveloper(authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return userService.findUsers(page, size, role, status);
    }
}
