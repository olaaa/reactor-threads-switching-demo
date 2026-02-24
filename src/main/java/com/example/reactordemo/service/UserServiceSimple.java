// src/main/java/com/example/reactordemo/service/UserServiceSimple.java
package com.example.reactordemo.service;

import com.example.reactordemo.entity.UserEntity;
import com.example.reactordemo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Простая реализация — почти без переключений, но с subscribeOn для эксперимента
 */
@Service("userServiceSimple")
@Slf4j
public class UserServiceSimple implements UserService {

    private final UserRepository userRepository;

    public UserServiceSimple(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Генерация — это момент, когда Reactor создаёт элемент (вызывает just, fromCallable, читает файл и т.д.).
     * В твоём случае:
     * <p>
     * Mono.just(user)н е генерирует ничего нового — он просто оборачивает уже существующий объект.
     * Поэтому генерация здесь почти мгновенная и происходит в том же потоке,
     */
    @Override
    public Mono<UserEntity> saveUser(UserEntity user) {
//        создаём новую цепочку
        return Mono.just(user)
// Thread: webflux-http-nio-2 --> boundedElastic-1
//  Без subscribeOn(): всё на одном потоке webflux-http-nio-2
                // subscribeOn влияет на всю цепочку, даже если поставить ниже → влияет на Mono.just и подписку
                .subscribeOn(Schedulers.boundedElastic())     // ← весь upstream на boundedElastic

                // webflux-http-nio-2
                .doOnSubscribe(sub -> log.info("Подписка: {}", Thread.currentThread().getName()))

                // Этот doOnNext уже выполняется на boundedElastic/loomBoundedElastic-1
                .doOnNext(u -> log.info("Элемент в doOnNext: {}", Thread.currentThread().getName()))

                // Сохранение — тоже на boundedElastic
                .flatMap(userRepository::save)

                // Отладка после сохранения boundedElastic-1/loomBoundedElastic-1
                .doOnNext(saved -> log.info("После сохранения: {}", Thread.currentThread().getName()))

                .log("UserServiceSimple");
    }
}