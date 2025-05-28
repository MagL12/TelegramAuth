package org.telegram.auth.telegramauth.dto;

import lombok.Data;

@Data
public class TelegramAuthRequest {
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String photoUrl;
    private Integer authDate;
    private String hash;
}
