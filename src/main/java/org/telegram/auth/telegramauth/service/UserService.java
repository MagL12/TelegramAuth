package org.telegram.auth.telegramauth.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.auth.telegramauth.dto.TelegramUserData;
import org.telegram.auth.telegramauth.model.TelegramUser;
import org.telegram.auth.telegramauth.repository.TelegramUserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private TelegramUserRepository userRepository;

    @Transactional
    public TelegramUser saveOrUpdateUser(TelegramUserData telegramUserData) {
        Optional<TelegramUser> existingUser = userRepository.findById(telegramUserData.getId());

        TelegramUser user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setUpdatedAt(LocalDateTime.now());
        } else {
            user = new TelegramUser(telegramUserData.getId());
        }

        // Обновляем данные пользователя
        user.setFirstName(telegramUserData.getFirstName());
        user.setLastName(telegramUserData.getLastName());
        user.setUsername(telegramUserData.getUsername());
        user.setLanguageCode(telegramUserData.getLanguageCode());
        user.setIsPremium(telegramUserData.getIsPremium());
        user.setAllowsWriteToPm(telegramUserData.getAllowsWriteToPm());

        return userRepository.save(user);
    }

    public Optional<TelegramUser> findById(Long telegramId) {
        return userRepository.findById(telegramId);
    }
}