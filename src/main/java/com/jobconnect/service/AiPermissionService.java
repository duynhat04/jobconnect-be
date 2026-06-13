package com.jobconnect.service;

import com.jobconnect.dto.ai.AiIntent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AiPermissionService {

    private static final String PUBLIC = "PUBLIC";

    public String resolveRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return PUBLIC;
        }

        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse(PUBLIC);
    }

    public void validatePermission(String role, AiIntent intent) {
        Set<AiIntent> allowed = getAllowedIntents(role);

        if (!allowed.contains(intent)) {
            throw new RuntimeException("Bạn không có quyền hỏi nội dung này!");
        }
    }

    private Set<AiIntent> getAllowedIntents(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return Set.of(
                    AiIntent.PUBLIC_JOB_SEARCH,
                    AiIntent.PUBLIC_STATS,
                    AiIntent.ADMIN_DASHBOARD_STATS,
                    AiIntent.ADMIN_PENDING_APPROVALS,
                    AiIntent.GENERAL_HELP
            );
        }

        if ("EMPLOYER".equalsIgnoreCase(role)) {
            return Set.of(
                    AiIntent.PUBLIC_JOB_SEARCH,
                    AiIntent.PUBLIC_STATS,
                    AiIntent.EMPLOYER_JOB_STATS,
                    AiIntent.EMPLOYER_APPLICATION_STATS,
                    AiIntent.GENERAL_HELP
            );
        }

        if ("CANDIDATE".equalsIgnoreCase(role)) {
            return Set.of(
                    AiIntent.PUBLIC_JOB_SEARCH,
                    AiIntent.PUBLIC_STATS,
                    AiIntent.CANDIDATE_APPLICATIONS,
                    AiIntent.CANDIDATE_PROFILE_REVIEW,
                    AiIntent.CANDIDATE_JOB_RECOMMENDATION,
                    AiIntent.GENERAL_HELP
            );
        }

        return Set.of(
                AiIntent.PUBLIC_JOB_SEARCH,
                AiIntent.PUBLIC_STATS,
                AiIntent.GENERAL_HELP
        );
    }
}