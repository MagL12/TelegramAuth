package org.telegram.auth.telegramauth.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
                model.addAttribute("errorTitle", "Ошибка аутентификации");
                model.addAttribute("errorMessage", "Данные аутентификации отсутствуют или недействительны. Пожалуйста, запустите приложение из Telegram.");
                model.addAttribute("errorCode", "401");
            } else if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("errorTitle", "Страница не найдена");
                model.addAttribute("errorMessage", "Запрашиваемая страница не существует.");
                model.addAttribute("errorCode", "404");
            } else {
                model.addAttribute("errorTitle", "Ошибка сервера");
                model.addAttribute("errorMessage", "Произошла внутренняя ошибка сервера. Попробуйте позже.");
                model.addAttribute("errorCode", String.valueOf(statusCode));
            }
        } else {
            model.addAttribute("errorTitle", "Неизвестная ошибка");
            model.addAttribute("errorMessage", "Произошла неизвестная ошибка.");
            model.addAttribute("errorCode", "500");
        }

        return "error";
    }
}