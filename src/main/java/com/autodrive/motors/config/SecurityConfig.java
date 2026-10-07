package com.autodrive.motors.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AdministradorProperties administrador, PasswordEncoder passwordEncoder) {
        var usuario = User.withUsername(administrador.getUsuario().trim())
                .password(passwordEncoder.encode(administrador.getContrasena()))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(usuario);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().hasRole("ADMIN"))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(this::iniciarAutenticacion)
                        .accessDeniedHandler(this::manejarAccesoDenegado))
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session.sessionFixation(sessionFixation -> sessionFixation.migrateSession()));
        return http.build();
    }

    private void iniciarAutenticacion(HttpServletRequest request, HttpServletResponse response,
                                      org.springframework.security.core.AuthenticationException ex) throws IOException, ServletException {
        if (esApi(request)) {
            responderJson(response, HttpServletResponse.SC_UNAUTHORIZED, "La sesión de administrador es obligatoria.");
            return;
        }
        new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, ex);
    }

    private void manejarAccesoDenegado(HttpServletRequest request, HttpServletResponse response,
                                       org.springframework.security.access.AccessDeniedException ex) throws IOException, ServletException {
        if (esApi(request)) {
            responderJson(response, HttpServletResponse.SC_FORBIDDEN, "La operación no está autorizada.");
            return;
        }
        new AccessDeniedHandlerImpl().handle(request, response, ex);
    }

    private boolean esApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    private void responderJson(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
