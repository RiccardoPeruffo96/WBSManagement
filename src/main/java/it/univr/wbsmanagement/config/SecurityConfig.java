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

/**
 *  SecurityConfig configures the security settings for the application.
 *  It defines which URLs are accessible without authentication,
 *  which require authentication, and how login/logout should be handled.
 */
@Configuration
public class SecurityConfig {

    /**
     * This bean registers the Spring Security dialect for Thymeleaf templates.
     * It allows us to use security-related attributes in our HTML templates.
     *
     * @return a new instance of SpringSecurityDialect
     */
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

    /**
     * This bean registers the Spring Security dialect for Thymeleaf templates.
     * It allows us to use security-related attributes in our HTML templates.
     *
     * @return a new instance of SpringSecurityDialect
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
