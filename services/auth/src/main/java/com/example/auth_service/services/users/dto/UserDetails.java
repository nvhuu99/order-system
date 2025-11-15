package com.example.auth_service.services.users.dto;

import com.example.auth_service.repositories.entities.User;
import com.example.auth_service.repositories.entities.UserRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDetails implements org.springframework.security.core.userdetails.UserDetails {

    String id;
    String username;
    String email;
    String password;
    String accessToken;
    String refreshToken;
    List<String> roles;


    public static UserDetails fromEntity(User entity, List<UserRole> roles) {
        var userDetails = fromEntity(entity);
        userDetails.setRoles(roles.stream().map(UserRole::getRole).toList());
        return userDetails;
    }

    public static UserDetails fromEntity(User entity) {
        var userDetails = new UserDetails();
        userDetails.setId(entity.getId());
        userDetails.setUsername(entity.getUsername());
        userDetails.setEmail(entity.getEmail());
        userDetails.setPassword(entity.getPassword());
        userDetails.setAccessToken(entity.getAccessToken());
        userDetails.setRefreshToken(entity.getRefreshToken());
        return userDetails;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
