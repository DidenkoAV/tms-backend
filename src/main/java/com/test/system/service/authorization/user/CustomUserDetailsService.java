package com.test.system.service.authorization.user;

import com.test.system.model.user.User;
import com.test.system.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 * Loads user details from database for authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private static final String LOG_PREFIX = "[UserDetailsService]";

    private final UserRepository users;

    /**
     * Loads user by email for Spring Security authentication.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = normEmail(email);

        User user = users.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.debug("{} user not found: '{}'", LOG_PREFIX, normalizedEmail);
                    return new UsernameNotFoundException("User not found: " + normalizedEmail);
                });

        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());

        log.trace("{} loaded user: email='{}', roles={}, enabled={}",
                LOG_PREFIX, normalizedEmail, authorities.size(), user.isEnabled());

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .build();
    }

    private static String normEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}

