package com.example.auth_service.repositories.users.configs;

import com.example.auth_service.repositories.users.entities.User;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class UserIdGenerator implements BeforeConvertCallback<User> {

    @Override
    public Publisher<User> onBeforeConvert(User entity, SqlIdentifier table) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        return Mono.just(entity);
    }
}
