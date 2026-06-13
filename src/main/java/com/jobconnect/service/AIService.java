package com.jobconnect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobconnect.exception.AIException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    @Value("${ai.request.timeout:30}")
    private int timeout;

    private final WebClient groqWebClient;
    private final ObjectMapper objectMapper;

    public String generateCoverLetter(
            String candidateName,
            String skills,
            String jobTitle,
            String companyName) {
        String prompt = """
                Bạn là ứng viên tên %s.
                Kỹ năng của ứng viên: %s
                Vị trí ứng tuyển: %s
                Công ty ứng tuyển: %s

                Hãy viết một Cover Letter ngắn gọn bằng tiếng Việt.
                Văn phong lịch sự, chuyên nghiệp.
                Độ dài khoảng 250-350 từ.
                Không bịa kinh nghiệm không được cung cấp.
                Chỉ trả về nội dung thư.
                """.formatted(candidateName, skills, jobTitle, companyName);

        return callGroq(prompt);
    }

    public String generateJobDescription(
            String title,
            String skills,
            String location) {
        String prompt = """
                Bạn là chuyên gia tuyển dụng tại Việt Nam.
                Hãy viết mô tả công việc chuyên nghiệp bằng tiếng Việt.

                Vị trí: %s
                Kỹ năng yêu cầu: %s
                Địa điểm làm việc: %s

                Trình bày theo format:
                1. Mô tả công việc
                2. Trách nhiệm chính
                3. Yêu cầu ứng viên
                4. Quyền lợi
                5. Thời gian và địa điểm làm việc

                Chỉ trả về nội dung mô tả công việc.
                """.formatted(title, skills, location);

        return callGroq(prompt);
    }

    public String chat(String message) {
        message = limitInput(message, 1000);

        String prompt = """
                Bạn là trợ lý AI của nền tảng tìm kiếm việc làm JobConnect.

                Nhiệm vụ:
                - Tư vấn tìm việc cho ứng viên
                - Gợi ý ngành nghề, kỹ năng, vị trí phù hợp
                - Hướng dẫn viết CV, Cover Letter
                - Trả lời ngắn gọn, dễ hiểu bằng tiếng Việt

                Câu hỏi của người dùng:
                %s

                Yêu cầu:
                - Trả lời thân thiện
                - Không trả lời lan man
                - Nếu thiếu thông tin, hãy hỏi thêm kỹ năng, kinh nghiệm, địa điểm mong muốn
                """.formatted(message);

        return callGroq(prompt);
    }

    public String suggestJobs(
            String skills,
            String experience,
            String location,
            String expectedSalary) {
        skills = limitInput(skills, 1000);
        experience = limitInput(experience, 500);
        location = limitInput(location, 300);
        expectedSalary = limitInput(expectedSalary, 300);

        String prompt = """
                Bạn là chuyên gia tư vấn nghề nghiệp cho nền tảng tìm kiếm việc làm.

                Thông tin ứng viên:
                - Kỹ năng: %s
                - Kinh nghiệm: %s
                - Địa điểm mong muốn: %s
                - Mức lương mong muốn: %s

                Hãy gợi ý việc làm phù hợp cho ứng viên.

                Format trả về:
                1. Tổng quan mức độ phù hợp
                2. Các vị trí công việc phù hợp
                3. Lý do phù hợp với từng vị trí
                4. Kỹ năng nên bổ sung
                5. Gợi ý cải thiện hồ sơ/CV

                Yêu cầu:
                - Trả lời bằng tiếng Việt
                - Thực tế, phù hợp thị trường tuyển dụng
                - Không bịa thông tin cá nhân của ứng viên
                """.formatted(
                skills,
                experience,
                location,
                expectedSalary == null || expectedSalary.isBlank() ? "Không cung cấp" : expectedSalary);

        return callGroq(prompt);
    }

    public String analyzeCv(
            String cvContent,
            String targetJobTitle) {
        cvContent = limitInput(cvContent, 5000);
        targetJobTitle = limitInput(targetJobTitle, 300);

        String prompt = """
                Bạn là chuyên gia nhân sự và tư vấn nghề nghiệp.

                Dưới đây là nội dung CV của ứng viên:
                %s

                Vị trí ứng tuyển mong muốn: %s

                Hãy phân tích CV này.

                Format trả về:
                1. Tóm tắt hồ sơ ứng viên
                2. Điểm mạnh của CV
                3. Điểm yếu cần cải thiện
                4. Các vị trí công việc phù hợp
                5. Kỹ năng nên bổ sung
                6. Gợi ý chỉnh sửa CV để chuyên nghiệp hơn

                Yêu cầu:
                - Trả lời bằng tiếng Việt
                - Không bịa kinh nghiệm không có trong CV
                - Nhận xét rõ ràng, dễ hiểu
                - Tập trung vào tuyển dụng và tìm việc
                """.formatted(
                cvContent,
                targetJobTitle == null || targetJobTitle.isBlank() ? "Không cung cấp" : targetJobTitle);

        return callGroq(prompt);
    }

    private String callGroq(String prompt) {
        try {
            String response = groqWebClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(buildRequest(prompt))
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 429,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> new AIException(
                                            "AI đang quá tải hoặc đã hết giới hạn miễn phí. Vui lòng thử lại sau.",
                                            HttpStatus.TOO_MANY_REQUESTS)))
                    .onStatus(
                            status -> status.value() == 401,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> new AIException(
                                            "API key Groq không hợp lệ hoặc đã hết quyền truy cập.",
                                            HttpStatus.UNAUTHORIZED)))
                    .onStatus(
                            status -> status.value() == 400,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> new AIException(
                                            "Yêu cầu gửi tới AI không hợp lệ. Vui lòng kiểm tra dữ liệu đầu vào.",
                                            HttpStatus.BAD_REQUEST)))
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(errorBody -> new AIException(
                                            "Dịch vụ AI đang gặp lỗi. Vui lòng thử lại sau.",
                                            HttpStatus.BAD_GATEWAY)))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeout))
                    .block();

            return parseResponse(response);

        } catch (AIException e) {
            throw e;
        } catch (Exception e) {
            log.error("Groq AI failed: {}", e.getMessage(), e);
            throw new AIException(
                    "Không thể kết nối tới dịch vụ AI. Vui lòng thử lại sau.",
                    HttpStatus.BAD_GATEWAY,
                    e);
        }
    }

    private GroqRequest buildRequest(String prompt) {
        return new GroqRequest(
                model,
                List.of(
                        new Message("system", "Bạn là trợ lý AI cho nền tảng tìm kiếm việc làm JobConnect."),
                        new Message("user", prompt)),
                0.7);
    }

    private String parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            if (root.has("error")) {
                throw new RuntimeException(root.get("error").toString());
            }

            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("Empty choices from Groq");
            }

            String content = choices
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (content == null || content.isBlank()) {
                throw new RuntimeException("Empty content from Groq");
            }

            return content;

        } catch (Exception e) {
            throw new RuntimeException("Cannot parse Groq response: " + e.getMessage(), e);
        }
    }

    private String limitInput(String input, int maxLength) {
        if (input == null) {
            return "";
        }

        if (input.length() <= maxLength) {
            return input;
        }

        return input.substring(0, maxLength);
    }

    public record GroqRequest(
            String model,
            List<Message> messages,
            double temperature) {
    }

    public record Message(
            String role,
            String content) {
    }

    public String generateAnswerFromPrompt(String prompt) {
        prompt = limitInput(prompt, 12000);
        return callGroq(prompt);
    }
}