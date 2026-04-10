package com.jobconnect.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // BẬT CORS
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                .authorizeHttpRequests(auth -> auth
                        // ==========================================
                        // 1. PUBLIC: Khách vãng lai cũng xem được
                        // ==========================================
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        // Cho phép xem danh sách/chi tiết Job và Công ty
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/jobs", "/api/jobs/**",
                                "/api/companies", "/api/companies/**").permitAll()
                        // Cho phép mở Swagger API
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // ==========================================
                        // 2. ADMIN: Quyền quản trị hệ thống
                        // ==========================================
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/companies/approve/**",
                                "/api/jobs/approve/**",
                                "/api/jobs/reject/**").hasAuthority("ADMIN")

                        // ==========================================
                        // 3. EMPLOYER: Quyền Nhà tuyển dụng
                        // ==========================================
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/jobs/my-jobs",
                                "/api/companies/my-company").hasAuthority("EMPLOYER")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/jobs", "/api/jobs/**").hasAuthority("EMPLOYER")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/jobs/**", "/api/companies/my-profile").hasAuthority("EMPLOYER")

                        // ==========================================
                        // 4. CANDIDATE: Quyền Ứng viên
                        // ==========================================
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/applications/**").hasAuthority("CANDIDATE")
                        .requestMatchers("/api/cv/**").hasAuthority("CANDIDATE")

                        // ==========================================
                        // 5. CÁC API KHÁC: Bắt buộc phải có Token hợp lệ
                        // ==========================================
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CẤU HÌNH CORS ĐỂ FRONTEND GỌI ĐƯỢC API
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://jobconnect-fe-nextjs.vercel.app"
        ));

        // Cho phép các phương thức gọi API
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Cho phép gửi kèm Token (Authorization)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));

        // Bắt buộc phải là true thì React mới gửi Token sang Spring Boot được
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng luật CORS này cho toàn bộ đường dẫn API (/**)
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}