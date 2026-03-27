package com.jobconnect.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "JobConnect API", version = "1.0", description = "Tài liệu hệ thống API Tuyển dụng"),
        security = @SecurityRequirement(name = "bearerAuth") // Áp dụng ổ khóa cho tất cả API
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {
    // Để trống thế này là đủ rồi, Spring Boot sẽ tự hiểu!
}