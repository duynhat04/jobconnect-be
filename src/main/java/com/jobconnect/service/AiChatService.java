package com.jobconnect.service;

import com.jobconnect.dto.ai.AiIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AIService aiService;
    private final AiIntentService aiIntentService;
    private final AiPermissionService aiPermissionService;
    private final AiDatabaseContextService aiDatabaseContextService;

    public String chat(String message, Authentication authentication) {
        if (!StringUtils.hasText(message)) {
            throw new RuntimeException("Nội dung tin nhắn không được để trống!");
        }

        if (message.length() > 1000) {
            throw new RuntimeException("Câu hỏi quá dài, vui lòng nhập ngắn gọn hơn!");
        }

        String role = aiPermissionService.resolveRole(authentication);
        String email = authentication != null ? authentication.getName() : null;

        AiIntent intent = aiIntentService.detectIntent(message);

        aiPermissionService.validatePermission(role, intent);

        String databaseContext = aiDatabaseContextService.buildContext(
                message,
                intent,
                role,
                email
        );

        String prompt = buildPrompt(message, role, intent, databaseContext);

        return aiService.generateAnswerFromPrompt(prompt);
    }

    private String buildPrompt(
            String userMessage,
            String role,
            AiIntent intent,
            String databaseContext
    ) {
        return """
                Bạn là trợ lý AI của nền tảng tìm việc JobConnect.

                QUY TẮC BẮT BUỘC:
                - Chỉ trả lời dựa trên DATABASE_CONTEXT nếu câu hỏi liên quan dữ liệu hệ thống.
                - Không bịa số liệu, không tự đoán dữ liệu không có.
                - Không tiết lộ mật khẩu, token, OTP, refresh token hoặc dữ liệu nhạy cảm.
                - Không trả lời vượt quyền của user.
                - Nếu không có dữ liệu phù hợp, hãy nói rõ chưa tìm thấy dữ liệu.
                - Trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu.

                ROLE_USER:
                %s

                INTENT:
                %s

                DATABASE_CONTEXT:
                %s

                CÂU HỎI USER:
                %s
                """.formatted(
                role,
                intent.name(),
                StringUtils.hasText(databaseContext) ? databaseContext : "Không có dữ liệu phù hợp.",
                userMessage
        );
    }
}