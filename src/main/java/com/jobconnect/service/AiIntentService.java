package com.jobconnect.service;

import com.jobconnect.dto.ai.AiIntent;
import org.springframework.stereotype.Service;

@Service
public class AiIntentService {

    public AiIntent detectIntent(String message) {
        String text = normalize(message);

        if (containsAny(text, "tôi đã ứng tuyển", "hồ sơ của tôi", "đơn ứng tuyển của tôi", "lịch sử ứng tuyển")) {
            return AiIntent.CANDIDATE_APPLICATIONS;
        }

        if (containsAny(text, "cv của tôi", "hồ sơ cá nhân", "profile của tôi", "thiếu thông tin")) {
            return AiIntent.CANDIDATE_PROFILE_REVIEW;
        }

        if (containsAny(text, "việc phù hợp", "gợi ý việc", "recommend job", "phù hợp với kỹ năng")) {
            return AiIntent.CANDIDATE_JOB_RECOMMENDATION;
        }

        if (containsAny(text, "công ty tôi", "tin của tôi", "tin tuyển dụng của tôi", "tin nào sắp hết hạn")) {
            return AiIntent.EMPLOYER_JOB_STATS;
        }

        if (containsAny(text, "cv đang chờ", "ứng viên của tôi", "hồ sơ ứng viên", "bao nhiêu ứng viên")) {
            return AiIntent.EMPLOYER_APPLICATION_STATS;
        }

        if (containsAny(text, "dashboard", "thống kê hệ thống", "tổng số user", "tổng số công ty")) {
            return AiIntent.ADMIN_DASHBOARD_STATS;
        }

        if (containsAny(text, "chờ duyệt", "cần duyệt", "pending approval")) {
            return AiIntent.ADMIN_PENDING_APPROVALS;
        }

        if (containsAny(text, "bao nhiêu việc", "thống kê việc", "có bao nhiêu job")) {
            return AiIntent.PUBLIC_STATS;
        }

        if (containsAny(text, "tìm việc", "việc làm", "job", "lương", "địa điểm", "java", "react", "marketing")) {
            return AiIntent.PUBLIC_JOB_SEARCH;
        }

        if (containsAny(text, "thông thạo", "thành thạo", "biết", "kỹ năng", "kinh nghiệm", "làm được")) {
            return AiIntent.PUBLIC_JOB_SEARCH;
        }
        return AiIntent.GENERAL_HELP;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

}