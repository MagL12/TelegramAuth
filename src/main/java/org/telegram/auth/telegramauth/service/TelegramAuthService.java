package org.telegram.auth.telegramauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.auth.telegramauth.dto.TelegramUserData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
@Slf4j
@Service
public class TelegramAuthService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean validateTelegramData(String initData) {
        try {
            Map<String, String> params = parseInitData(initData);
            log.info("Received initData: {}", initData);

            // Проверяем срок действия (не более 1 часа)
            String authDate = params.get("auth_date");
            if (authDate == null) {
                log.warn("auth_date is missing");
                return false;
            }

            long authTimestamp = Long.parseLong(authDate);
            long currentTimestamp = Instant.now().getEpochSecond();

            // Проверяем, что данные не старше 1 часа (3600 секунд)
            if (currentTimestamp - authTimestamp > 3600) {
                log.warn("auth_date is too old: {}", authTimestamp);
                return false;
            }

            // Проверяем hash
            String receivedHash = params.get("hash");
            if (receivedHash == null) {
                log.warn("hash is missing");
                return false;
            }

            // Удаляем hash из параметров для валидации
            params.remove("hash");

            // Создаем строку для проверки
            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));

            // Вычисляем секретный ключ
            byte[] secretKey = createSecretKey(botToken);

            // Вычисляем hash
            String calculatedHash = calculateHash(dataCheckString, secretKey);

            if (!receivedHash.equals(calculatedHash)) {
                log.warn("Hash mismatch - received: {}, calculated: {}", receivedHash, calculatedHash);
                return false;
            }
            log.info("initData validation successful");
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public TelegramUserData extractUserData(String initData) {
        try {
            Map<String, String> params = parseInitData(initData);
            String userJson = params.get("user");

            if (userJson != null) {
                return objectMapper.readValue(userJson, TelegramUserData.class);
            }

            return null;
        } catch (JsonProcessingException e) {
            log.error("Failed to extract user data: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> params = new TreeMap<>();

        String[] pairs = initData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } catch (Exception e) {
                    log.warn("Failed to decode initData parameter: {}", pair, e);
                }
            }
        }

        return params;
    }

    private byte[] createSecretKey(String botToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(botToken.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to create secret key", e);
            throw new RuntimeException("Failed to create secret key", e);
        }
    }

    private String calculateHash(String data, byte[] secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Конвертируем в hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to calculate hash", e);
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }
}