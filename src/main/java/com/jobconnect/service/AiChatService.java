package com.jobconnect.service;

import com.jobconnect.dto.ai.AiIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final AIService aiService;
    private final AiIntentService aiIntentService;
    private final AiPermissionService aiPermissionService;
    private final AiDatabaseContextService aiDatabaseContextService;

    public String chat(String message, Authentication authentication) {
        String safeMessage = normalizeMessage(message);

        String role = aiPermissionService.resolveRole(authentication);
        String email = authentication != null && authentication.isAuthenticated()
                ? authentication.getName()
                : null;

        AiIntent intent = aiIntentService.detectIntent(safeMessage);

        aiPermissionService.validatePermission(role, intent);

        String databaseContext = aiDatabaseContextService.buildContext(
                safeMessage,
                intent,
                role,
                email
        );

        String prompt = buildPrompt(
                safeMessage,
                role,
                intent,
                databaseContext
        );

        return aiService.generateAnswerFromPrompt(prompt);
    }

    private String buildPrompt(
            String userMessage,
            String role,
            AiIntent intent,
            String databaseContext
    ) {
        String safeDatabaseContext = StringUtils.hasText(databaseContext)
                ? databaseContext
                : "Không có dữ liệu phù hợp.";

        return """
                Bạn là trợ lý AI của nền tảng tìm việc JobConnect.

                QUY TẮC BẮT BUỘC:
                - Chỉ trả lời dựa trên DATABASE_CONTEXT nếu câu hỏi liên quan đến dữ liệu hệ thống.
                - Không bịa số liệu, không tự đoán dữ liệu không có trong DATABASE_CONTEXT.
                - Không tiết lộ mật khẩu, token, OTP, refresh token hoặc dữ liệu nhạy cảm.
                - Không trả lời vượt quyền của user.
                - Nếu không có dữ liệu phù hợp, hãy nói rõ là chưa tìm thấy dữ liệu phù hợp.
                - Trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu.
                - Không hiển thị các trường kỹ thuật như ID nếu không cần thiết.
                - Không tự tạo link nếu DATABASE_CONTEXT không có Link chi tiết.
                - Khi gợi ý việc làm, nếu DATABASE_CONTEXT có Link chi tiết, hãy hiển thị tên việc làm dưới dạng markdown link:
                  [Tên việc làm](/jobs/id)
                - Không được tự bịa id job. Chỉ dùng đúng id và Link chi tiết có trong DATABASE_CONTEXT.

                VÍ DỤ CÁCH TRẢ LỜI KHI CÓ LINK:
                Dựa trên hồ sơ của bạn, tôi gợi ý một số việc làm phù hợp:
                - [Lập trình viên Backend Java Spring Boot](/jobs/12) tại VIETCOMBANK
                - [Senior dev Java](/jobs/15) tại VIETCOMBANK

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
                safeDatabaseContext,
                userMessage
        );
    }

    private String normalizeMessage(String message) {
        if (!StringUtils.hasText(message)) {
            throw new RuntimeException("Nội dung tin nhắn không được để trống!");
        }

        String safeMessage = message.trim();

        if (safeMessage.length() > MAX_MESSAGE_LENGTH) {
            throw new RuntimeException("Câu hỏi quá dài, vui lòng nhập ngắn gọn hơn!");
        }

        return safeMessage;
    }
}