package org.telegram.auth.telegramauth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "telegram_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramUser {

    @Id
    private Long id;

    @Column(nullable = false)
    private String firstName;

    private String lastName;

    private String username;

    private String photoUrl;

    @Column(nullable = false)
    private Long authDate;

    @Column(nullable = false)
    private Long lastVisit;
}