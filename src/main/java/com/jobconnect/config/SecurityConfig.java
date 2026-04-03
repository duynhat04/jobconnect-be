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
                // BẬT CORS
                .cors(Customizer.withDefaults())

                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        // 1. PUBLIC: Khách vãng lai chưa đăng nhập cũng xem được
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()

                        // BẢO VỆ 2 API RIÊNG TƯ CỦA NHÀ TUYỂN DỤNG
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/jobs/my-jobs",
                                "/api/companies/my-company").hasAuthority("EMPLOYER")
                        // 1.2: Cho phép xem danh sách/chi tiết Job và Công ty
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/jobs", "/api/jobs/**",
                                "/api/companies", "/api/companies/**").permitAll()

                        // 2. ADMIN: Quyền quản trị hệ thống (Duyệt/Từ chối)
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/api/companies/approve/**",
                                "/api/jobs/approve/**",
                                "/api/jobs/reject/**").hasAuthority("ADMIN")

                        // 2.1: CHỈ "EMPLOYER" (Nhà tuyển dụng) mới được tạo/sửa Job
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/jobs", "/api/jobs/**").hasAuthority("EMPLOYER")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/jobs/**", "/api/companies/my-profile").hasAuthority("EMPLOYER")

                        // 2.3: CHỈ "CANDIDATE" (Ứng viên) mới được nộp CV
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/applications/**").hasAuthority("CANDIDATE")

                        // 3. Cho phép mở cửa riêng cho giao diện Swagger API
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 4. Các thao tác khác  thì cứ đăng nhập là được
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