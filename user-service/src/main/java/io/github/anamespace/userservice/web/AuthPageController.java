package io.github.anamespace.userservice.web;

import io.github.anamespace.userservice.dto.RegisterRequest;
import io.github.anamespace.userservice.exception.EmailAlreadyExistsException;
import io.github.anamespace.userservice.exception.UsernameAlreadyExistsException;
import io.github.anamespace.userservice.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Objects;

/**
 * Serves the browser-facing sign-in and registration pages of the authorization
 * server. The pages are only reachable through the gateway.
 */
@Controller
public class AuthPageController {

    private final AuthService authService;

    public AuthPageController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/signin")
    public String signIn() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(@ModelAttribute("form") RegisterForm form, BindingResult bindingResult) {
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult bindingResult,
                           HttpServletResponse response) {
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Пароли не совпадают.");
        }

        if (bindingResult.hasErrors()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "register";
        }

        try {
            authService.register(new RegisterRequest(
                    form.getUsername().trim(),
                    form.getEmail().trim(),
                    form.getPassword()
            ));
        } catch (UsernameAlreadyExistsException exception) {
            bindingResult.rejectValue("username", "username.exists", "Это имя уже занято.");
            response.setStatus(HttpStatus.CONFLICT.value());
            return "register";
        } catch (EmailAlreadyExistsException exception) {
            bindingResult.rejectValue("email", "email.exists", "Этот email уже используется.");
            response.setStatus(HttpStatus.CONFLICT.value());
            return "register";
        }

        return "redirect:/signin?registered";
    }
}
