// src/main/java/com/example/reactordemo/service/UserServiceWithThreadSwitch.java
package com.example.reactordemo.service;

import com.example.reactordemo.entity.UserEntity;
import com.example.reactordemo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Реализация с явным управлением потоками + subscribeOn и publishOn
 */
@Service("userServiceWithThreads")
@Slf4j
public class UserServiceWithThreadSwitch implements UserService {

    private final UserRepository userRepository;

    public UserServiceWithThreadSwitch(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserEntity> saveUser(UserEntity user) {
        // subscribeOn влияет на всю цепочку вверх (генерацию источника и подписку)
        // Здесь мы говорим: "подписка и выполнение Mono.just + flatMap должны происходить на parallel-потоке"
// Thread: webflux-http-nio-2
        return Mono.just(user) // запуск цепочки
//        parallel-2
                .doOnNext(u -> log.info("[генерация]: {}", Thread.currentThread().getName()))
                .subscribeOn(Schedulers.parallel())           // ← весь upstream → parallel

 // выполняется на том потоке, на котором вызвали .subscribe()
// Кто-то вызывает .subscribe() (в WebFlux это делает Netty на своём event-loop потоке — webflux-http-nio-2 или reactor-http-nio-2)
//  doOnSubscribe() — это side-effect на момент подписки, он должен сработать немедленно
//                webflux-http-nio-2
                .doOnSubscribe(sub -> log.info("[subscribe] Подписка в сервисе: {}", Thread.currentThread().getName()))

                // Отладка: обработка элемента после Mono.just (уже на parallel-2, т.к. subscribeOn выше)
//                parallel-2
                .doOnNext(u -> log.info("[just → doOnNext] Элемент после just: {}", Thread.currentThread().getName()))

                // Переключаем downstream (операции ниже) на другой пул
                // parallel-2 --> boundedElastic-1
                .publishOn(Schedulers.boundedElastic())

                // Отладка: теперь уже на boundedElastic-1/loomBoundedElastic-1
                .doOnNext(u -> log.info("[publishOn → doOnNext] Перед сохранением: {}", Thread.currentThread().getName()))

                // Сохранение в БД (R2DBC) — будет на boundedElastic/loomBoundedElastic-1
                .flatMap(userRepository::save)

                // Отладка: после сохранения — всё ещё boundedElastic/loomBoundedElastic-1
                .doOnNext(saved -> log.info("[after save] После сохранения: {}", Thread.currentThread().getName()))

                .log("UserServiceWithThreads");
    }
}