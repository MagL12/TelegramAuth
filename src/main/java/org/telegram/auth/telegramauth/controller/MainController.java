package org.telegram.auth.telegramauth.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.auth.telegramauth.dto.TelegramUserData;
import org.telegram.auth.telegramauth.model.TelegramUser;
import org.telegram.auth.telegramauth.service.TelegramAuthService;
import org.telegram.auth.telegramauth.service.UserService;

import java.util.Map;

@Controller
public class MainController {

    @Autowired
    private TelegramAuthService telegramAuthService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String index(@RequestHeader(value = "X-Init-Data", required = false) String initData, Model model) {

        // Если нет initData, возвращаем ошибку
        if (initData == null || initData.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication data");
        }

        // Валидируем данные Telegram
        if (!telegramAuthService.validateTelegramData(initData)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication data");
        }

        // Извлекаем данные пользователя
        TelegramUserData telegramUserData = telegramAuthService.extractUserData(initData);
        if (telegramUserData == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot extract user data");
        }

        // Сохраняем или обновляем пользователя в базе данных
        TelegramUser user = userService.saveOrUpdateUser(telegramUserData);

        // Добавляем данные в модель для отображения
        model.addAttribute("user", user);

        return "index";
    }

    @GetMapping("/health")
    public String health() {
        return "health";
    }
}