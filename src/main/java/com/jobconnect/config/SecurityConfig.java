package com.jobconnect.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer; // Thêm cái này
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // Thêm cái này
import org.springframework.web.cors.CorsConfigurationSource; // Thêm cái này
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Thêm cái này

import java.util.Arrays; // Thêm cái này

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
                // 🚀 BẬT CORS (Thay vì disable như cũ)
                .cors(Customizer.withDefaults())

                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                .authorizeHttpRequests(auth -> auth
                        // 1. Công khai: Ai cũng vào được
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()

                        // CHỈ CHO PHÉP TẤT CẢ MỌI NGƯỜI LẤY DỮ LIỆU (GET) TỪ JOB
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/jobs/**").permitAll()

                        // 2. Chỉ ADMIN mới được duyệt Công ty và duyệt/từ chối Job (PUT)
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/companies/approve/**",
                                "/api/jobs/approve/**",
                                "/api/jobs/reject/**").hasAuthority("ADMIN")

                        // 3. Cho phép mở cửa riêng cho Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 4. Các yêu cầu khác chỉ cần đăng nhập là được
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🚀 CẤU HÌNH CORS ĐỂ FRONTEND GỌI ĐƯỢC API
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ⚠️ NHỚ SỬA: Thay link Vercel của m vào đây nhé!
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