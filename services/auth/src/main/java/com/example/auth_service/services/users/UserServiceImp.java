package com.example.auth_service.services.users;

import com.example.auth_service.repositories.users.UserRolesCrudRepository;
import com.example.auth_service.repositories.users.entities.User;
import com.example.auth_service.repositories.users.entities.UserRole;
import com.example.auth_service.services.users.dto.SaveUser;
import com.example.auth_service.services.users.dto.UserDetails;
import com.example.auth_service.repositories.users.UsersCrudRepository;
import com.example.auth_service.services.users.exceptions.EmailDuplicationException;
import com.example.auth_service.services.users.exceptions.UsernameDuplicationException;
import com.example.auth_service.utils.auth_jwt.exceptions.TokenRejectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class UserServiceImp implements com.example.auth_service.services.users.UserService, org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UsersCrudRepository userCrudRepo;

    @Autowired
    private UserRolesCrudRepository userRolesCrudRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findByUsername(username).block();
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return Mono
            .zip(
                userCrudRepo.findByUsername(username),
                userRolesCrudRepo.findByUsername(username).collectList()
            )
            .map(t2 -> UserDetails.fromEntity(t2.getT1(), t2.getT2()))
        ;
    }

    @Override
    public Mono<UserDetails> findByAccessToken(String accessToken) {
        var userRef = new AtomicReference<User>();
        return userCrudRepo
            .findByAccessToken(accessToken)
            .switchIfEmpty(Mono.error(new TokenRejectedException()))
            .flatMap(user -> {
              userRef.set(user);
              return userRolesCrudRepo
                .findByUsername(user.getUsername())
                .collectList()
              ;
            })
            .map(roles -> UserDetails.fromEntity(userRef.get(), roles))
        ;
    }

    @Override
    public Mono<UserDetails> findByAccessTokenAndRefreshToken(String accessToken, String refreshToken) {
        var userRef = new AtomicReference<User>();
        return userCrudRepo
            .findByAccessTokenAndRefreshToken(accessToken, refreshToken)
            .switchIfEmpty(Mono.error(new TokenRejectedException()))
            .flatMap(user -> {
                userRef.set(user);
                return userRolesCrudRepo
                    .findByUsername(user.getUsername())
                    .collectList()
                    ;
            })
            .map(roles -> UserDetails.fromEntity(userRef.get(), roles))
        ;
    }

    @Override
    public Mono<UserDetails> save(SaveUser data) {
        var user = new User();
        user.setId(data.getId());
        user.setUsername(data.getUsername());
        user.setEmail(data.getEmail());
        user.setPassword(passwordEncoder.encode(data.getPassword()));

        var userRoles = new ArrayList<UserRole>();
        for (var roleName: data.getRoles()) {
            var ur = new UserRole();
            ur.setRole(roleName);
            ur.setUsername(data.getUsername());
            userRoles.add(ur);
        }

        var ensureUniqueUsernameAndEmail = Mono.when(
            checkUsernameNotTaken(data.getUsername()),
            checkEmailNotTaken(data.getEmail())
        );
        var saveUserAndUserRoles = Mono.zip(
            userCrudRepo.save(user),
            userRolesCrudRepo.saveAll(userRoles).collectList()
        );

        return Mono
            .just(data.getId() == null || data.getId().isEmpty())
            .flatMap(isInsert -> isInsert
                ? ensureUniqueUsernameAndEmail.then(saveUserAndUserRoles)
                : saveUserAndUserRoles
            )
            .map(t2 -> UserDetails.fromEntity(t2.getT1(), t2.getT2()))
        ;
    }

    @Override
    public Mono<Void> saveTokens(String username, String accessToken, String refreshToken) {
        return userCrudRepo
            .findByUsername(username)
            .flatMap(user -> {
                user.setAccessToken(accessToken);
                user.setRefreshToken(refreshToken);
                return userCrudRepo.save(user);
            })
            .then()
        ;
    }


    private Mono<Void> checkUsernameNotTaken(String username) {
        return userCrudRepo
            .existsByUsername(username)
            .flatMap(existed -> existed
                ? Mono.error(new UsernameDuplicationException())
                : Mono.empty()
            )
        ;
    }

    private Mono<Void> checkEmailNotTaken(String email) {
        return userCrudRepo
            .existsByEmail(email)
            .flatMap(existed -> existed
                ? Mono.error(new EmailDuplicationException())
                : Mono.empty()
            )
        ;
    }
}
