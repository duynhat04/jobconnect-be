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

                                                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                                                .permitAll()
                                                // ==========================================
                                                // 1. PUBLIC: Khách vãng lai cũng xem được
                                                // ==========================================
                                                .requestMatchers("/api/settings",
                                                                "/error")
                                                .permitAll()

                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/jobs",
                                                                "/api/jobs/**",
                                                                "/api/companies", "/api/companies/**",
                                                                "/api/v1/packages/active",
                                                                "/api/users/verify-otp",
                                                                "/api/dev/**")
                                                .permitAll()

                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/users/register",
                                                                "/api/users/login",
                                                                "/api/users/google-login",
                                                                "/api/users/verify-otp",
                                                                "/api/users/resend-otp",
                                                                "/api/ai/**")
                                                .permitAll()
                                                // Cho phép mở Swagger API
                                                .requestMatchers("/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                // ==========================================
                                                // 2. ADMIN: Quyền quản trị hệ thống
                                                // ==========================================
                                                .requestMatchers("/api/admin/**", "/api/v1/packages/**")
                                                .hasAuthority("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                                                "/api/companies/approve/**",
                                                                "/api/jobs/approve/**",
                                                                "/api/jobs/reject/**")
                                                .hasAuthority("ADMIN")

                                                // ==========================================
                                                // 3. EMPLOYER: Quyền Nhà tuyển dụng
                                                // ==========================================
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/jobs/my-jobs",
                                                                "/api/companies/my-company")
                                                .hasAuthority("EMPLOYER")
                                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/jobs",
                                                                "/api/jobs/**")
                                                .hasAuthority("EMPLOYER")
                                                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                                                "/api/jobs/**", "/api/companies/my-profile")
                                                .hasAuthority("EMPLOYER")

                                                // ==========================================
                                                // 4. CANDIDATE: Quyền Ứng viên
                                                // ==========================================
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/applications/**")
                                                .hasAuthority("CANDIDATE")
                                                .requestMatchers("/api/cv/**").hasAuthority("CANDIDATE")

                                                // ==========================================
                                                // 5. CÁC API KHÁC: Bắt buộc phải có Token hợp lệ
                                                // ==========================================
                                                .anyRequest().authenticated());

                http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        // CẤU HÌNH CORS ĐỂ FRONTEND GỌI ĐƯỢC API (DÙNG ĐỂ DEV LOCAL)
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(Arrays.asList(
                                "http://localhost:3000", 
                                "https://jobconnect-fe-nextjs.vercel.app" 
                ));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}