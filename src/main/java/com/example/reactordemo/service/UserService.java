// src/main/java/com/example/reactordemo/service/UserService.java
package com.example.reactordemo.service;

import com.example.reactordemo.entity.UserEntity;
import reactor.core.publisher.Mono;

/**
 * Интерфейс сервиса по работе с пользователями
 */
public interface UserService {

    /**
     * Сохраняет пользователя в базу данных
     *
     * @param user сущность пользователя
     * @return Mono с сохранённым пользователем
     */
    Mono<UserEntity> saveUser(UserEntity user);
}