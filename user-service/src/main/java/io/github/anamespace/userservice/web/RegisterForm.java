package io.github.anamespace.userservice.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Backing object for the registration page. Kept mutable because Spring MVC
 * binds form fields into it.
 */
public class RegisterForm {

    @NotBlank(message = "Введите имя пользователя.")
    @Size(min = 3, max = 50, message = "Имя должно быть от 3 до 50 символов.")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$",
            message = "Допустимы латинские буквы, цифры, точка, дефис и подчёркивание.")
    private String username;

    @NotBlank(message = "Введите email.")
    @Email(message = "Укажите корректный email.")
    @Size(max = 255, message = "Email слишком длинный.")
    private String email;

    @NotBlank(message = "Введите пароль.")
    @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов.")
    private String password;

    @NotBlank(message = "Повторите пароль.")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
