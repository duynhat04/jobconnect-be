package com.jobconnect.controller;

import com.jobconnect.dto.*;
import com.jobconnect.service.AIService;
import com.jobconnect.service.AiChatService;
import com.jobconnect.service.AIRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

        private final AIService aiService;
        private final AIRateLimitService aiRateLimitService;
        private final AiChatService aiChatService;

        @PostMapping("/generate-jd")
        public ResponseEntity<ApiResponse<AiGenerationResponse>> generateJobDescription(
                        @Valid @RequestBody AiJobDescriptionRequest request,
                        HttpServletRequest httpRequest) {
                aiRateLimitService.checkLimit(getClientIp(httpRequest));

                String result = aiService.generateJobDescription(
                                request.getTitle(),
                                request.getSkills(),
                                request.getLocation());

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                new AiGenerationResponse(result),
                                                "Tạo mô tả công việc thành công"));
        }

        @PostMapping("/generate-cover-letter")
        public ResponseEntity<ApiResponse<AiGenerationResponse>> generateCoverLetter(
                        @Valid @RequestBody AiCoverLetterRequest request,
                        HttpServletRequest httpRequest) {
                aiRateLimitService.checkLimit(getClientIp(httpRequest));

                String result = aiService.generateCoverLetter(
                                request.getCandidateName(),
                                request.getSkills(),
                                request.getJobTitle(),
                                request.getCompanyName());

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                new AiGenerationResponse(result),
                                                "Tạo Cover Letter thành công"));
        }

        @PostMapping("/chat")
        public ResponseEntity<ApiResponse<AiGenerationResponse>> chat(
                        @Valid @RequestBody AiChatRequest request,
                        HttpServletRequest httpRequest,
                        Authentication authentication) {
                String rateLimitKey = authentication != null && authentication.isAuthenticated()
                                ? authentication.getName()
                                : getClientIp(httpRequest);

                aiRateLimitService.checkLimit(rateLimitKey);

                String result = aiChatService.chat(
                                request.getMessage(),
                                authentication);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                new AiGenerationResponse(result),
                                                "Chatbot trả lời thành công"));
        }

        @PostMapping("/suggest-jobs")
        public ResponseEntity<ApiResponse<AiGenerationResponse>> suggestJobs(
                        @Valid @RequestBody AiJobSuggestionRequest request,
                        HttpServletRequest httpRequest) {
                aiRateLimitService.checkLimit(getClientIp(httpRequest));

                String result = aiService.suggestJobs(
                                request.getSkills(),
                                request.getExperience(),
                                request.getLocation(),
                                request.getExpectedSalary());

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                new AiGenerationResponse(result),
                                                "Gợi ý việc làm thành công"));
        }

        @PostMapping("/analyze-cv")
        public ResponseEntity<ApiResponse<AiGenerationResponse>> analyzeCv(
                        @Valid @RequestBody AiCvAnalysisRequest request,
                        HttpServletRequest httpRequest) {
                aiRateLimitService.checkLimit(getClientIp(httpRequest));

                String result = aiService.analyzeCv(
                                request.getCvContent(),
                                request.getTargetJobTitle());

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                new AiGenerationResponse(result),
                                                "Phân tích CV thành công"));
        }

        private String getClientIp(HttpServletRequest request) {
                String forwardedFor = request.getHeader("X-Forwarded-For");

                if (forwardedFor != null && !forwardedFor.isBlank()) {
                        return forwardedFor.split(",")[0].trim();
                }

                return request.getRemoteAddr();
        }
}