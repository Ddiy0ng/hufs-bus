package hufsbus.spring.domain.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/terms/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/timetable").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/timetable/**").hasAuthority("DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/driver/location").hasAuthority("DRIVER")
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/timetables/**").authenticated()
                        .requestMatchers("/api/driver/**").authenticated()
                        .requestMatchers("/api/buses/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                            Map<String, Object> body = new LinkedHashMap<>();
                            body.put("message", "인증이 필요합니다.");
                            body.put("data", null);
                            body.put("localDateTime", LocalDateTime.now().toString());
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                            Map<String, Object> body = new LinkedHashMap<>();
                            body.put("message", "접근 권한이 없습니다.");
                            body.put("data", null);
                            body.put("localDateTime", LocalDateTime.now().toString());
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
