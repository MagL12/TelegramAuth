package org.telegram.auth.telegramauth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.telegram.auth.telegramauth.model.TelegramUser;

public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {
}