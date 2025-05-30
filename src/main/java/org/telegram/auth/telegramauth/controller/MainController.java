package org.telegram.auth.telegramauth.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.auth.telegramauth.dto.TelegramUserData;
import org.telegram.auth.telegramauth.model.TelegramUser;
import org.telegram.auth.telegramauth.service.TelegramAuthService;
import org.telegram.auth.telegramauth.service.UserService;

import java.util.Map;

@Slf4j
@Controller
public class MainController {
    @Autowired
    private TelegramAuthService telegramAuthService;
    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String index(@RequestParam(value = "tgWebAppData", required = false) String initData, Model model) {
        log.info("Received request with initData: {}", initData);

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
        model.addAttribute("user", user);

        return "index";
    }

    @GetMapping("/health")
    public String health() {
        return "health";
    }
}