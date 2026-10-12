package ru.practicum.gateway.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.List;

@EnableWebFluxSecurity
@Configuration
@EnableConfigurationProperties(SecurityConfig.UserProperties.class)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
                                                            UserProperties properties) {
        List<UserDetails> users = properties.getUsers().stream()
                .map(user -> User.builder()
                        .username(user.getUsername())
                        .password(passwordEncoder.encode(user.getPassword()))
                        .roles(user.getRoles().toArray(new String[0])).build())
                .toList();

        return new MapReactiveUserDetailsService(users);
    }

    @Bean
    public SecurityWebFilterChain apiSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/inventory/**",
                                "/api/categories/**"
                        ).permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/orders").hasRole("USER")
                        .pathMatchers(HttpMethod.GET,
                                "/api/orders/by-email?...",
                                "/api/orders/{id}")
                        .hasRole("USER")
                        .pathMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/products/{id}").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/inventory").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST,
                                "/api/products",
                                "/api/categories",
                                "/api/inventory"
                        ).hasRole("ADMIN")
                        .anyExchange().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .cors(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @ConfigurationProperties(prefix = "app.security")
    public static class UserProperties {
        private List<Users> users;

        public List<Users> getUsers() {
            return users;
        }
        public void setUsers(List<Users> users) {
            this.users = users;
        }

        public static class Users {
            private String username;
            private String password;
            private List<String> roles;

            public String getUsername() {
                return username;
            }
            public void setUsername(String username) {
                this.username = username;
            }
            public String getPassword() {
                return password;
            }
            public void setPassword(String password) {
                this.password = password;
            }
            public List<String> getRoles() {
                return roles;
            }
            public void setRoles(List<String> roles) {
                this.roles = roles;
            }
        }
    }
}