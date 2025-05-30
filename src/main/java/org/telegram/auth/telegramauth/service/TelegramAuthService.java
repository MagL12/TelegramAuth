package org.telegram.auth.telegramauth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.auth.telegramauth.dto.TelegramUserData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramAuthService {

    @Value("${telegram.bot.token}")
    private String botToken;

    public boolean validateTelegramData(String initData) {
        try {
            Map<String, String> params = parseInitData(initData);
            log.info("Parsed initData params: {}", params);

            String authDate = params.get("auth_date");
            if (authDate == null) {
                log.warn("auth_date is missing");
                return false;
            }

            long authTimestamp = Long.parseLong(authDate);
            long currentTimestamp = System.currentTimeMillis() / 1000;
            if (currentTimestamp - authTimestamp > 3600) {
                log.warn("auth_date is too old: {}", authTimestamp);
                return false;
            }

            String receivedHash = params.get("hash");
            if (receivedHash == null) {
                log.warn("hash is missing");
                return false;
            }

            // Создаем копию параметров и удаляем hash и signature
            Map<String, String> dataParams = new HashMap<>(params);
            dataParams.remove("hash");
            dataParams.remove("signature");

            // Создаем data check string согласно документации Telegram
            String dataCheckString = createDataCheckString(dataParams);

            log.info("Data check string: '{}'", dataCheckString);

            byte[] secretKey = createSecretKey(botToken);
            String calculatedHash = calculateHash(dataCheckString, secretKey);

            log.info("Calculated hash: {}, Received hash: {}", calculatedHash, receivedHash);

            if (!receivedHash.equals(calculatedHash)) {
                log.warn("Hash mismatch. Calculated: {}, Received: {}", calculatedHash, receivedHash);
                return false;
            }

            log.info("initData validation successful");
            return true;
        } catch (Exception e) {
            log.error("Validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private String createDataCheckString(Map<String, String> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // Для параметра user нужно декодировать URL-encoding
                    if ("user".equals(key)) {
                        try {
                            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
                            // Удаляем экранирование слешей
                            value = value.replace("\\/", "/");
                        } catch (Exception e) {
                            log.warn("Failed to decode user parameter: {}", e.getMessage());
                        }
                    }

                    return key + "=" + value;
                })
                .collect(Collectors.joining("\n"));
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> params = new HashMap<>();
        if (initData == null || initData.isEmpty()) {
            return params;
        }

        for (String pair : initData.split("&")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    private byte[] createSecretKey(String botToken) throws Exception {
        log.info("Using bot token: {}", botToken);
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(keySpec);
        byte[] secretKey = hmacSha256.doFinal(botToken.getBytes(StandardCharsets.UTF_8));
        log.info("Secret key: {}", bytesToHex(secretKey));
        return secretKey;
    }

    private String calculateHash(String data, byte[] secretKey) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey, "HmacSHA256");
        hmacSha256.init(keySpec);
        byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public TelegramUserData extractUserData(String initData) {
        try {
            Map<String, String> params = parseInitData(initData);
            String userParam = params.get("user");

            if (userParam == null) {
                log.warn("User parameter is missing from initData");
                return null;
            }

            // Декодируем URL-encoded JSON
            String decodedUserJson = URLDecoder.decode(userParam, StandardCharsets.UTF_8);
            log.info("Decoded user JSON: {}", decodedUserJson);

            // Парсим JSON (используйте вашу библиотеку JSON, например Jackson или Gson)
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode userNode = objectMapper.readTree(decodedUserJson);

            TelegramUserData userData = new TelegramUserData();
            userData.setId(userNode.get("id").asLong());
            userData.setFirstName(userNode.get("first_name").asText());

            if (userNode.has("last_name") && !userNode.get("last_name").isNull()) {
                userData.setLastName(userNode.get("last_name").asText());
            }

            if (userNode.has("username") && !userNode.get("username").isNull()) {
                userData.setUsername(userNode.get("username").asText());
            }

            if (userNode.has("language_code") && !userNode.get("language_code").isNull()) {
                userData.setLanguageCode(userNode.get("language_code").asText());
            }

            if (userNode.has("allows_write_to_pm")) {
                userData.setAllowsWriteToPm(userNode.get("allows_write_to_pm").asBoolean());
            }

            if (userNode.has("is_premium")) {
                userData.setIsPremium(userNode.get("is_premium").asBoolean());
            }

            return userData;

        } catch (Exception e) {
            log.error("Failed to extract user data: {}", e.getMessage(), e);
            return null;
        }
    }
}