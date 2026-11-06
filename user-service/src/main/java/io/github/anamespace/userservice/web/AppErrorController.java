package io.github.anamespace.userservice.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Branded replacement for Spring Boot's Whitelabel error page.
 *
 * <p>Authorization-server failures can end up as a direct request to
 * {@code /error} (for example when an OAuth2 redirect URI is not registered).
 * Such a request carries no error attributes, so Boot falls back to a
 * meaningless "status 999" Whitelabel page. Normalize that case to 400 and
 * render the same page as every other error.</p>
 */
@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String error(HttpServletRequest request, HttpServletResponse response, Model model) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = statusAttribute instanceof Integer code
                ? HttpStatus.resolve(code)
                : null;

        if (status == null || status.value() == 999) {
            // A direct hit on /error without an active error dispatch. This is
            // usually a failed authorization request, so report it as a bad
            // request instead of the container's internal 999 sentinel.
            status = HttpStatus.BAD_REQUEST;
            response.setStatus(status.value());
        }

        model.addAttribute("status", status.value());
        model.addAttribute("title", title(status));
        model.addAttribute("message", message(status));
        model.addAttribute("path", path(request));
        return "error";
    }

    private String title(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "Некорректный запрос";
            case 401 -> "Требуется вход";
            case 403 -> "Нет доступа";
            case 404 -> "Страница не найдена";
            case 500 -> "Внутренняя ошибка";
            default -> "Ошибка";
        };
    }

    private String message(HttpStatus status) {
        return switch (status.value()) {
            case 400 -> "Ссылка авторизации устарела или содержит неверные параметры. "
                    + "Вернитесь на главную страницу и попробуйте войти снова.";
            case 401 -> "Для доступа к этой странице нужно войти.";
            case 403 -> "У вашей учётной записи нет прав на этот раздел.";
            case 404 -> "Запрошенная страница не существует или была перемещена.";
            case 500 -> "Что-то пошло не так на стороне сервера. Попробуйте ещё раз чуть позже.";
            default -> "Не удалось выполнить запрос. Вернитесь на главную страницу.";
        };
    }

    private String path(HttpServletRequest request) {
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return uri instanceof String value ? value : request.getRequestURI();
    }
}
