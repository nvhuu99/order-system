package com.example.auth_service.repositories.users.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRoleAuthorities {

    private static final Map<String, List<String>> authoritiesMap = new HashMap<>();
    static {
        authoritiesMap.put(Role.ADMIN.getName(), List.of(
            "/"
        ));
        authoritiesMap.put(Role.CUSTOMER.getName(), List.of(
            "/api/v1/carts"
        ));
    }

    public static Boolean hasAuthority(String role, String resourcePrefix) {
        if (! authoritiesMap.containsKey(role)) return false;
        for (var authority: authoritiesMap.get(role)) {
            if (resourcePrefix.startsWith(authority)) {
                return true;
            }
        }
        return false;
    }
}
