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
                                .cors(Customizer.withDefaults())
                                .httpBasic(basic -> basic.disable())
                                .formLogin(form -> form.disable())

                                .authorizeHttpRequests(auth -> auth

                                                // ==========================================
                                                // 0. OPTIONS: Cho phép preflight CORS
                                                // ==========================================
                                                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                                                .permitAll()

                                                // ==========================================
                                                // 1. PUBLIC: API công khai
                                                // ==========================================
                                                .requestMatchers(
                                                                "/api/settings",
                                                                "/error",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/users/register",
                                                                "/api/users/login",
                                                                "/api/users/google-login",
                                                                "/api/users/verify-otp",
                                                                "/api/users/resend-otp",
                                                                "/api/users/forgot-password",
                                                                "/api/users/reset-password")
                                                .permitAll()

                                                // ==========================================
                                                // 2. ADMIN: Quyền quản trị hệ thống
                                                // ==========================================
                                                .requestMatchers("/api/admin/**")
                                                .hasAuthority("ADMIN")

                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/packages/active")
                                                .permitAll()

                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/packages/**")
                                                .hasAnyAuthority("ADMIN", "EMPLOYER")

                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/v1/packages/**")
                                                .hasAuthority("ADMIN")

                                                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                                                "/api/v1/packages/**")
                                                .hasAuthority("ADMIN")

                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/v1/packages/**")
                                                .hasAuthority("ADMIN")

                                                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                                                "/api/companies/approve/**",
                                                                "/api/jobs/approve/**",
                                                                "/api/jobs/reject/**")
                                                .hasAuthority("ADMIN")

                                                // ==========================================
                                                // 3. EMPLOYER: Quyền Nhà tuyển dụng
                                                // Đặt trước public GET /api/jobs/*
                                                // ==========================================
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/jobs/my-jobs",
                                                                "/api/companies/my-company",
                                                                "/api/companies/my-stats",
                                                                "/api/applications/job/*",
                                                                "/api/applications/job/*/candidates",
                                                                "/api/applications/employer/all")
                                                .hasAnyAuthority("EMPLOYER", "ADMIN")

                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/jobs",
                                                                "/api/jobs/**")
                                                .hasAuthority("EMPLOYER")

                                                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                                                "/api/jobs/**",
                                                                "/api/companies/my-profile",
                                                                "/api/applications/*/status")
                                                .hasAnyAuthority("EMPLOYER", "ADMIN")

                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/jobs/**")
                                                .hasAnyAuthority("EMPLOYER", "ADMIN")

                                                // ==========================================
                                                // 4. CANDIDATE: Quyền Ứng viên
                                                // ==========================================
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/applications/my-applications")
                                                .hasAuthority("CANDIDATE")

                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/applications/apply",
                                                                "/api/applications/apply-new",
                                                                "/api/applications/apply-existing")
                                                .hasAuthority("CANDIDATE")

                                                .requestMatchers("/api/cv/**")
                                                .hasAuthority("CANDIDATE")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/users/saved-jobs",
                                                                "/api/users/saved-jobs/**")
                                                .hasAnyAuthority("CANDIDATE", "ADMIN")

                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/users/saved-jobs/**")
                                                .hasAnyAuthority("CANDIDATE", "ADMIN")

                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/users/saved-jobs/**")
                                                .hasAnyAuthority("CANDIDATE", "ADMIN")
                                                // ==========================================
                                                // 5. USER ĐÃ ĐĂNG NHẬP: Profile, đổi mật khẩu, thông báo, AI
                                                // ==========================================
                                                .requestMatchers(
                                                                "/api/users/profile",
                                                                "/api/users/change-password",
                                                                "/api/users/refresh-token",
                                                                "/api/users/avatar",
                                                                "/api/notifications/**")
                                                .authenticated()

                                                // ==========================================
                                                // 5. AI: Phân quyền theo từng chức năng
                                                // ==========================================

                                                // Chatbot AI có thể dùng cho user đã đăng nhập.
                                                // Bên trong AiChatService sẽ tự giới hạn dữ liệu theo role.
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/ai/chat")
                                                .authenticated()

                                                // Nhà tuyển dụng tạo mô tả công việc bằng AI
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/ai/generate-jd")
                                                .hasAnyAuthority("EMPLOYER", "ADMIN")

                                                // Ứng viên dùng AI để viết cover letter, phân tích CV, gợi ý việc
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/ai/generate-cover-letter",
                                                                "/api/ai/suggest-jobs",
                                                                "/api/ai/analyze-cv")
                                                .hasAnyAuthority("CANDIDATE", "ADMIN")

                                                // ==========================================
                                                // 6. PUBLIC GET: Khách vãng lai xem được
                                                // Đặt sau các API protected để tránh public nhầm
                                                // ==========================================
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/jobs",
                                                                "/api/jobs/*",
                                                                "/api/jobs/**",
                                                                "/api/companies",
                                                                "/api/companies/*",
                                                                "/api/companies/**",
                                                                "/api/news",
                                                                "/api/news/**",
                                                                "/api/public/**",
                                                                "/api/dev/**")

                                                .permitAll()

                                                // ==========================================
                                                // 7. CÁC API KHÁC: Bắt buộc có token hợp lệ
                                                // ==========================================
                                                .anyRequest().authenticated());

                http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOriginPatterns(Arrays.asList(
                                "http://localhost:3000",
                                "https://jobconnect-fe-nextjs.vercel.app",
                                "https://*.vercel.app"));

                configuration.setAllowedMethods(Arrays.asList(
                                "GET",
                                "POST",
                                "PUT",
                                "PATCH",
                                "DELETE",
                                "OPTIONS"));

                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}