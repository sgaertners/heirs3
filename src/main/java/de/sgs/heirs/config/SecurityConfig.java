package de.sgs.heirs.config;

import de.sgs.heirs.service.ServerMasterKeyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

        http
                // 1. Session-Repository
                .securityContext(context -> context.securityContextRepository(securityContextRepository()))

                // 2. CSRF für Login & QR-Login deaktivieren (verhindert Login-Abbrüche durch Token-Mismatch)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/qr-login", "/login", "/master"))

                // 3. Nur bei echtem 403 (unberechtigter Zugriff auf geschützte URL) abmelden
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                                logoutHandler.logout(request, response, auth);
                                SecurityContextHolder.clearContext();
                            }
                            response.sendRedirect(request.getContextPath() + "/logout?error=access_denied");
                        })
                )

                // 4. URL-Berechtigungen
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/verwalter/**").hasRole("VERWALTER") // <-- NEU: Exklusiver Notar-Pfad
                        .requestMatchers("/master", "/login", "/qr-auth", "/api/qr-login", "/wyarf/", "/wyarfList/**", "/wyarfdelete/**", "/error",
                                "/css/**", "/js/**", "/webjars/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )

                // 5. Form-Login (Standard-Ablauf von Spring Security nicht stören!)
                .formLogin(form -> form
                        .loginPage("/master")
                        .loginProcessingUrl("/master")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/logout?error=bad_credentials")
                        .permitAll()
                )

                // 6. Logout
                .logout(logout -> logout
                        .logoutSuccessUrl("/logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Liest beim Neustart die Datei wyarf.irs aus, entschlüsselt alle Key/Value-Paare
     * und übergibt die Benutzer als Liste an den InMemoryUserDetailsManager.
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder, ServerMasterKeyService masterKeyService) {
        List<UserDetails> userList = new ArrayList<>();
        Path lockPath = Paths.get("wyarf.irs");
        Path altLockPath = Paths.get("wyaf.irs");
        Path activePath = Files.exists(lockPath) ? lockPath : (Files.exists(altLockPath) ? altLockPath : null);

        if (activePath != null) {
            try (BufferedReader reader = Files.newBufferedReader(activePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || !line.contains("=")) continue;

                    String[] parts = line.split("=", 2);
                    String encUser = parts[0].trim();
                    String encPass = parts[1].trim();

                    // Mit MasterKey entschlüsseln:
                    String decryptedUsername = masterKeyService.decryptWithMasterKey(encUser);
                    String decryptedPassword = masterKeyService.decryptWithMasterKey(encPass);

                    userList.add(User.builder()
                            .username(decryptedUsername)
                            .password(encoder.encode(decryptedPassword))
                            .roles("ADMIN")
                            .build());
                }
                System.out.println(">>> " + userList.size() + " Admin-Benutzer aus " + activePath.getFileName() + " entschlüsselt und geladen.");
            } catch (Exception e) {
                System.err.println("Fehler beim Laden von " + activePath.getFileName() + ": " + e.getMessage());
            }
        }

        // Fallback: Falls noch keine Datei existiert (Erststart vor /action)
        if (userList.isEmpty()) {
            userList.add(User.builder()
                    .username("admin")
                    .password(encoder.encode("admin123"))
                    .roles("ADMIN")
                    .build());
        }

        return new InMemoryUserDetailsManager(userList);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }
}