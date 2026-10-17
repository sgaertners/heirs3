package de.sgs.heirs.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/qr-login")) // CSRF für AJAX-Login deaktivieren
                .authorizeHttpRequests(auth -> auth
                        // 1. Admin-Bereich exklusiv für ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/master", "/qr-auth", "/api/qr-login", "/css/**", "/js/**", "/webjars/**").permitAll() // <-- /webjars/** hinzugefügt
                        // 2. Öffentliche Login-Routen
                        .requestMatchers("/login", "/api/qr-login", "/css/**", "/js/**").permitAll()
                        // 3. Alles andere erfordert Authentifizierung (User oder Admin)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/master")
                        .defaultSuccessUrl("/dashboard", true)
                )
                .logout(logout -> logout.logoutSuccessUrl("/master?logout"));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Fester Admin-Account für die Verwaltung
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123")) // In Produktion über Vault/Env
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }
}