package io.github.anamespace.userservice.web;

import io.github.anamespace.userservice.dto.UpdateEmailRequest;
import io.github.anamespace.userservice.dto.UpdatePasswordRequest;
import io.github.anamespace.userservice.dto.UpdateProfileRequest;
import io.github.anamespace.userservice.entity.UserStatus;
import io.github.anamespace.userservice.exception.EmailAlreadyExistsException;
import io.github.anamespace.userservice.exception.InvalidCurrentPasswordException;
import io.github.anamespace.userservice.exception.RoleConflictException;
import io.github.anamespace.userservice.service.UserService;
import io.github.anamespace.userservice.util.AvatarColors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Browser-facing settings page. It uses the authorization-server session
 * instead of a bearer token, because it is rendered for the same user that
 * signed in through the gateway.
 */
@Controller
@RequestMapping("/settings")
public class SettingsPageController {

    private final UserService userService;

    public SettingsPageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String settings(Authentication authentication, Model model) {
        model.addAttribute("profile", userService.getProfile(authentication.getName()));
        model.addAttribute("palette", AvatarColors.PALETTE);
        model.addAttribute("active", UserStatus.ACTIVE.name());
        return "settings";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
                                @RequestParam(required = false) String displayName,
                                @RequestParam(required = false) String avatarColor,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(authentication.getName(),
                    new UpdateProfileRequest(displayName, avatarColor));
            redirectAttributes.addFlashAttribute("message", "Профиль обновлён.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/email")
    public String updateEmail(Authentication authentication,
                              @RequestParam String email,
                              RedirectAttributes redirectAttributes) {
        try {
            userService.updateEmail(authentication.getName(), new UpdateEmailRequest(email));
            redirectAttributes.addFlashAttribute("message", "Email обновлён.");
        } catch (EmailAlreadyExistsException exception) {
            redirectAttributes.addFlashAttribute("error", "Этот email уже используется.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", "Укажите корректный email.");
        }
        return "redirect:/settings";
    }

    @PostMapping("/password")
    public String updatePassword(Authentication authentication,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.updatePassword(authentication.getName(),
                    new UpdatePasswordRequest(currentPassword, newPassword));
            redirectAttributes.addFlashAttribute("message", "Пароль обновлён.");
        } catch (InvalidCurrentPasswordException exception) {
            redirectAttributes.addFlashAttribute("error", "Текущий пароль указан неверно.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", "Новый пароль должен быть не короче 8 символов.");
        }
        return "redirect:/settings";
    }

    @PostMapping("/become-developer")
    public String becomeDeveloper(Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.becomeDeveloper(authentication.getName());
            redirectAttributes.addFlashAttribute("message", "Роль DEVELOPER добавлена.");
        } catch (RoleConflictException exception) {
            redirectAttributes.addFlashAttribute("error", "Администратор не может получить роль разработчика.");
        }
        return "redirect:/settings";
    }
}
