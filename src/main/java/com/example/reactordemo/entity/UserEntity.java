// src/main/java/com/example/reactordemo/entity/UserEntity.java
package com.example.reactordemo.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("users")
public class UserEntity {
    @Id
    private Long id;
    private String name;
    private String email;
}