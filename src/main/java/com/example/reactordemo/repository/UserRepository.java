// src/main/java/com/example/reactordemo/repository/UserRepository.java
package com.example.reactordemo.repository;

import com.example.reactordemo.entity.UserEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserRepository extends ReactiveCrudRepository<UserEntity, Long> {
}