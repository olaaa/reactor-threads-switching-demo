// src/test/java/com/example/reactordemo/controller/UserControllerSimpleTest.java
package com.example.reactordemo.controller;

import com.example.reactordemo.ReactorDemoApplication;
import com.example.reactordemo.entity.UserEntity;
import com.example.reactordemo.repository.UserRepository;
import com.example.reactordemo.service.UserService;
import com.example.reactordemo.service.UserServiceSimple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.scheduler.Schedulers;

@SpringBootTest(
        classes = {ReactorDemoApplication.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@PropertySource("classpath:application.yml")
class UserControllerSimpleTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void enableVirtualThreadsForReactor() {
//        VM options !!! а не свойство приложения
        System.setProperty("reactor.schedulers.defaultBoundedElasticOnVirtualThreads", "true");
        // Важно: после этого можно принудительно пересоздать boundedElastic
        Schedulers.resetFactory();  // ← сброс кэша schedulers
    }

    @Test
    void shouldCreateUser_withSimpleService() {
        UserEntity request = new UserEntity();
        request.setName("Simple Ivan");
        request.setEmail("simple.ivan@example.com");

        webTestClient.post()
                .uri("/users")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserEntity.class)
                .consumeWith(result -> {
                    UserEntity saved = result.getResponseBody();
                    assert saved != null;
                    assert saved.getId() != null : "ID должен быть сгенерирован";
                    assert "Simple Ivan".equals(saved.getName());
                    assert "simple.ivan@example.com".equals(saved.getEmail());
                });
    }

}