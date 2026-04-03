package com.itexpert.content.core;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                .password("{noop}Admin13579++")
                .roles("ADMIN", "EDITOR")
                .build();
        return new MapReactiveUserDetailsService(admin);
    }
}