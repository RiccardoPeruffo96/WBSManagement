package it.univr.wbsmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

import it.univr.wbsmanagement.database.DatabaseManager;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
//import it.univr.wbsmanagement.models.User;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/forgot-password", "/css/**", "/js/**", "/images/**").permitAll() // Here we permit access to the login page, root path, CSS, JS, images, etc.
                        .requestMatchers("/admin/**").hasRole("Administrator") // Only Administrators may access /admin/**
                        .anyRequest().authenticated() // Any other request requires authentication
                )
                .formLogin(form -> form
                        .loginPage("/login") // Our custom login page
                        .defaultSuccessUrl("/homepage", true) // Redirect after the login
                        .failureUrl("/login?error")        // Redirect to /login?error on failure
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    /*
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin")
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user);
    }
    */
    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                // Example: search in table 'users'

                var userRow = DatabaseManager.getUserRowByEmail(username);
                if (userRow == null) {
                    throw new UsernameNotFoundException("User not found: " + username);
                }

                String email = userRow.get("email");
                String password = userRow.get("password");
                String roleName = userRow.get("role_name");

                var authority = new SimpleGrantedAuthority("ROLE_" + roleName);

                return User.builder()
                        .username(email)
                        .password("{noop}" + password) // not encryption
                        .authorities(Collections.singletonList(authority))
                        .build();
            }
        };
    }
}
