package org.telegram.auth.telegramauth.controller;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.auth.telegramauth.dto.TelegramUserData;
import org.telegram.auth.telegramauth.model.TelegramUser;
import org.telegram.auth.telegramauth.service.TelegramAuthService;
import org.telegram.auth.telegramauth.service.UserService;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class MainController {
    @Autowired
    private TelegramAuthService telegramAuthService;
    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String index(Model model) {
        log.info("Received request to render index page");
        // Пока не добавляем user в модель, это сделаем через AJAX
        return "index";
    }

    @PostMapping("/validate")
    @ResponseBody
    public Map<String, Object> validate(@RequestBody Map<String, String> request) {
        String initData = request.get("initData");
        log.info("Received initData for validation: {}", initData);

        if (initData == null || initData.isEmpty()) {
            log.warn("Missing authentication data");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication data");
        }

        if (!telegramAuthService.validateTelegramData(initData)) {
            log.warn("Invalid authentication data");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication data");
        }

        TelegramUserData telegramUserData = telegramAuthService.extractUserData(initData);
        if (telegramUserData == null) {
            log.warn("Cannot extract user data");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot extract user data");
        }

        TelegramUser user = userService.saveOrUpdateUser(telegramUserData);
        log.info("User saved/updated: {}", user);

        // Возвращаем данные пользователя для отображения на фронте
        Map<String, Object> response = new HashMap<>();
        response.put("telegramId", user.getTelegramId());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("username", user.getUsername());
        response.put("languageCode", user.getLanguageCode());
        response.put("isPremium", user.getIsPremium());
        response.put("allowsWriteToPm", user.getAllowsWriteToPm());
        response.put("createdAt", user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        response.put("updatedAt", user.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        return response;
    }

    @GetMapping("/health")
    public String health() {
        return "health";
    }
}