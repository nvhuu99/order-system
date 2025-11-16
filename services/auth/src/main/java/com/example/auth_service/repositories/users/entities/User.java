package com.example.auth_service.repositories.users.entities;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @Column("id")
    String id;

    @Column("username")
    String username;

    @Column("email")
    String email;

    @Column("password")
    String password;

    @Column("access_token")
    String accessToken;

    @Column("refresh_token")
    String refreshToken;
}
