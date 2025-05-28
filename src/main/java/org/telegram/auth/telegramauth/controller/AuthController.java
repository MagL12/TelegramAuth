package org.telegram.auth.telegramauth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.auth.telegramauth.dto.TelegramAuthRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/telegram")
    public ResponseEntity<String> telegramAuth(@RequestBody TelegramAuthRequest request) {
        System.out.println("Получили запрос: " + request);
        return ResponseEntity.ok("OK");
    }
}