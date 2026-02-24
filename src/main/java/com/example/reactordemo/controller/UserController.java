// src/main/java/com/example/reactordemo/controller/UserController.java
package com.example.reactordemo.controller;

import com.example.reactordemo.entity.UserEntity;
import com.example.reactordemo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class UserController {

    private final UserService userService;

    // Выбираем нужную реализацию через @Qualifier
//    public UserController(@Qualifier("userServiceWithThreads") UserService userService) {
    public UserController(@Qualifier("userServiceSimple") UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserEntity> createUser(@RequestBody UserEntity user) {
        return Mono.just(user)
//                webflux-http-nio-2 вне зависимости от реализации сервиса
                .doOnSubscribe(sub -> log.info("Subscribed in controller: {}", Thread.currentThread().getName()))
                .doOnNext(u -> log.info("Received in controller: {}", Thread.currentThread().getName()))
                .flatMap(userService::saveUser)
// boundedElastic-1/loomBoundedElastic-1 вне зависимости от реализации сервиса
                .doOnNext(saved -> log.info("Returning from controller: {}", Thread.currentThread().getName()))
                .log();
    }
}