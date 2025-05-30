package org.telegram.auth.telegramauth.service;

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

    private final Object macLock = new Object();

    public boolean validateTelegramData(String initData) {
        try {
            log.info("Raw initData: {}", initData);
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

            Map<String, String> dataParams = new HashMap<>(params);
            dataParams.remove("hash");
            dataParams.remove("signature");

            Map<String, String> decodedParams = new HashMap<>();
            for (Map.Entry<String, String> entry : dataParams.entrySet()) {
                try {
                    String decodedValue = URLDecoder.decode(entry.getValue(), StandardCharsets.UTF_8);
                    if ("user".equals(entry.getKey()) && decodedValue.contains("\\/")) {
                        decodedValue = decodedValue.replace("\\/", "/");
                        log.debug("Replaced escaped slashes in user: {} -> {}", entry.getValue(), decodedValue);
                    }
                    decodedParams.put(entry.getKey(), decodedValue);
                    log.debug("Decoded {}: {} -> {}", entry.getKey(), entry.getValue(), decodedValue);
                } catch (Exception e) {
                    log.warn("Failed to decode parameter {}: {}", entry.getKey(), e.getMessage());
                    decodedParams.put(entry.getKey(), entry.getValue());
                }
            }

            String dataCheckString = decodedParams.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));

            dataCheckString = dataCheckString.trim();
            log.info("Trimmed data check string: '{}'", dataCheckString);

            log.info("Data check string (byte length): {}", dataCheckString.getBytes(StandardCharsets.UTF_8).length);
            log.info("Data check string (char array): {}", dataCheckString.toCharArray());
            log.info("Data check string: '{}'", dataCheckString);

            String[] lines = dataCheckString.split("\n");
            for (int i = 0; i < lines.length; i++) {
                log.info("Line {}: '{}'", i, lines[i]);
            }

            byte[] secretKey = createSecretKey(botToken);
            log.info("Secret key (hex): {}", bytesToHex(secretKey));
            String calculatedHash = calculateHash(dataCheckString, secretKey);
            log.info("Calculated hash: {}, Received hash: {}", calculatedHash, receivedHash);

            if (!receivedHash.equals(calculatedHash)) {
                log.warn("Hash mismatch. Calculated: {}, Received: {}", calculatedHash, receivedHash);

                String rawDataCheckString = dataParams.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining("\n"));
                String rawCalculatedHash = calculateHash(rawDataCheckString, secretKey);
                log.info("Raw (no decode) data check string: '{}'", rawDataCheckString);
                log.info("Raw calculated hash: {}", rawCalculatedHash);

                return false;
            }

            log.info("initData validation successful");
            return true;
        } catch (Exception e) {
            log.error("Validation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> params = new HashMap<>();
        for (String pair : initData.split("&")) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    private byte[] createSecretKey(String botToken) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec botTokenKeySpec = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(botTokenKeySpec);
        byte[] secretKey = hmacSha256.doFinal("WebAppData".getBytes(StandardCharsets.UTF_8));
        return secretKey;
    }

    private String calculateHash(String data, byte[] secretKey) throws Exception {
        synchronized (macLock) {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            hmacSha256.init(keySpec);
            hmacSha256.reset();
            byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        }
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
        return new TelegramUserData();
    }
}