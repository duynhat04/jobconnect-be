package com.jobconnect.service;

import com.jobconnect.dto.ai.AiIntent;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import com.jobconnect.repository.CompanyRepository;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiDatabaseContextService {

    private static final int MAX_ITEMS = 10;
    private static final int MAX_SEARCH_CANDIDATES = 100;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "toi", "t", "minh", "em", "anh", "chi", "ban",
            "muon", "can", "dang", "co", "biet", "thong", "thao",
            "thanh", "kinh", "nghiem", "ky", "nang",
            "tim", "viec", "lam", "cong", "job",
            "cho", "giup", "voi", "ve", "la", "o", "tai",
            "nao", "gi", "khong", "duoc", "phu", "hop"
    );

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobApplicationRepository jobApplicationRepository;

    @Transactional(readOnly = true)
    public String buildContext(
            String message,
            AiIntent intent,
            String role,
            String email
    ) {
        return switch (intent) {
            case PUBLIC_JOB_SEARCH -> buildPublicJobSearchContext(message);
            case PUBLIC_STATS -> buildPublicStatsContext();

            case CANDIDATE_APPLICATIONS -> buildCandidateApplicationsContext(email);
            case CANDIDATE_PROFILE_REVIEW -> buildCandidateProfileContext(email);
            case CANDIDATE_JOB_RECOMMENDATION -> buildCandidateRecommendationContext(email);

            case EMPLOYER_JOB_STATS -> buildEmployerJobStatsContext(email);
            case EMPLOYER_APPLICATION_STATS -> buildEmployerApplicationStatsContext(email);

            case ADMIN_DASHBOARD_STATS -> buildAdminDashboardContext();
            case ADMIN_PENDING_APPROVALS -> buildAdminPendingContext();

            default -> "";
        };
    }

    // =========================
    // PUBLIC CONTEXT
    // =========================

    private String buildPublicJobSearchContext(String message) {
        List<Job> jobs = findBestMatchingPublicJobs(message);

        if (jobs.isEmpty()) {
            return "Không tìm thấy tin tuyển dụng công khai phù hợp với nội dung người dùng hỏi.";
        }

        StringBuilder context = new StringBuilder(
                "Danh sách việc làm công khai phù hợp, chỉ gồm tin đã duyệt và còn hạn:\n"
        );

        for (Job job : jobs) {
            appendJobLine(context, job);
        }

        return context.toString();
    }

    private String buildPublicStatsContext() {
        long activeJobs = jobRepository.countActivePublicJobs();
        long companies = companyRepository.countByStatus("APPROVED");
        long candidates = userRepository.countByRole("CANDIDATE");

        return """
                Thống kê công khai của hệ thống:
                - Việc làm đang tuyển: %d
                - Doanh nghiệp đã được duyệt: %d
                - Ứng viên trên hệ thống: %d
                """.formatted(activeJobs, companies, candidates);
    }

    // =========================
    // CANDIDATE CONTEXT
    // =========================

    private String buildCandidateApplicationsContext(String email) {
        User user = getUserByEmail(email);

        List<JobApplication> applications =
                jobApplicationRepository.findByUserIdOrderByAppliedAtDesc(user.getId());

        if (applications.isEmpty()) {
            return "Ứng viên hiện chưa ứng tuyển công việc nào.";
        }

        StringBuilder context = new StringBuilder(
                "Danh sách hồ sơ ứng tuyển của chính ứng viên đang đăng nhập:\n"
        );

        applications.stream()
                .limit(MAX_ITEMS)
                .forEach(app -> {
                    Job job = app.getJob();

                    context.append("- ")
                            .append(job != null ? safe(job.getTitle()) : "Không rõ job")
                            .append(" | Công ty: ")
                            .append(job != null && job.getCompany() != null
                                    ? safe(job.getCompany().getName())
                                    : "Không rõ")
                            .append(" | Trạng thái hồ sơ: ")
                            .append(toApplicationStatusText(app.getStatus()))
                            .append(" | Trạng thái tin: ")
                            .append(job != null ? toJobStatusText(job) : "Không rõ")
                            .append(" | Ngày ứng tuyển: ")
                            .append(app.getAppliedAt() != null ? app.getAppliedAt() : "Không rõ")
                            .append("\n");
                });

        return context.toString();
    }

    private String buildCandidateProfileContext(String email) {
        User user = getUserByEmail(email);

        return """
                Hồ sơ của chính ứng viên đang đăng nhập:
                - Họ tên: %s
                - Email: %s
                - Số điện thoại: %s
                - Kỹ năng: %s
                - Vị trí mong muốn: %s
                - Ngành nghề mong muốn: %s
                - Kinh nghiệm: %s
                - Lương mong muốn: %s
                - Học vấn: %s
                - Tiếng Anh: %s
                - Chứng chỉ: %s
                - Dự án: %s
                """.formatted(
                safe(user.getFullName()),
                safe(user.getEmail()),
                safe(user.getPhone()),
                safe(user.getSkills()),
                safe(user.getDesiredPosition()),
                safe(user.getDesiredCategory()),
                user.getExperienceYears() == null
                        ? "Chưa cập nhật"
                        : user.getExperienceYears() + " năm",
                user.getExpectedSalary() == null
                        ? "Chưa cập nhật"
                        : formatSalary(user.getExpectedSalary()),
                safe(user.getEducationLevel()),
                safe(user.getEnglishLevel()),
                safe(user.getCertificates()),
                safe(user.getProjects())
        );
    }

    private String buildCandidateRecommendationContext(String email) {
        User user = getUserByEmail(email);

        String profileSearchText = String.join(" ",
                safeToEmpty(user.getDesiredPosition()),
                safeToEmpty(user.getSkills()),
                safeToEmpty(user.getDesiredCategory()),
                safeToEmpty(user.getEducationLevel()),
                safeToEmpty(user.getEnglishLevel())
        );

        List<Job> jobs = findBestMatchingPublicJobs(profileSearchText);

        if (jobs.isEmpty()) {
            return "Chưa tìm thấy việc làm phù hợp với hồ sơ ứng viên.";
        }

        StringBuilder context = new StringBuilder(
                "Việc làm công khai được gợi ý cho ứng viên dựa trên hồ sơ cá nhân:\n"
        );

        for (Job job : jobs) {
            appendJobLine(context, job);
        }

        return context.toString();
    }

    // =========================
    // EMPLOYER CONTEXT
    // =========================

    private String buildEmployerJobStatsContext(String email) {
        requireLogin(email);

        List<Job> jobs = jobRepository.findMyJobsWithCompany(email);

        if (jobs.isEmpty()) {
            return "Nhà tuyển dụng hiện chưa có tin tuyển dụng nào.";
        }

        long active = jobs.stream()
                .filter(job -> "APPROVED".equalsIgnoreCase(job.getStatus()))
                .filter(job -> !isExpired(job))
                .filter(job -> !isClosed(job))
                .count();

        long pending = jobs.stream()
                .filter(job -> "PENDING".equalsIgnoreCase(job.getStatus()))
                .count();

        long rejected = jobs.stream()
                .filter(job -> "REJECTED".equalsIgnoreCase(job.getStatus()))
                .count();

        long expired = jobs.stream()
                .filter(this::isExpired)
                .count();

        long closed = jobs.stream()
                .filter(this::isClosed)
                .count();

        StringBuilder context = new StringBuilder();

        context.append("Thống kê tin tuyển dụng của chính nhà tuyển dụng đang đăng nhập:\n")
                .append("- Tổng tin: ").append(jobs.size()).append("\n")
                .append("- Đang tuyển: ").append(active).append("\n")
                .append("- Chờ duyệt: ").append(pending).append("\n")
                .append("- Bị từ chối/khóa: ").append(rejected).append("\n")
                .append("- Hết hạn: ").append(expired).append("\n")
                .append("- Đã đóng: ").append(closed).append("\n")
                .append("Một số tin gần đây:\n");

        jobs.stream()
                .limit(MAX_ITEMS)
                .forEach(job -> {
                    long applicationCount = jobApplicationRepository.countByJobId(job.getId());

                    context.append("- ")
                            .append(safe(job.getTitle()))
                            .append(" | Trạng thái: ")
                            .append(toJobStatusText(job))
                            .append(" | Hạn: ")
                            .append(formatDate(job.getExpiredAt()))
                            .append(" | Số CV: ")
                            .append(applicationCount)
                            .append("\n");
                });

        return context.toString();
    }

    private String buildEmployerApplicationStatsContext(String email) {
        requireLogin(email);

        List<JobApplication> applications =
                jobApplicationRepository.findByJob_Company_User_EmailOrderByAppliedAtDesc(email);

        if (applications.isEmpty()) {
            return "Chưa có ứng viên nào ứng tuyển vào công ty của nhà tuyển dụng này.";
        }

        long pending = applications.stream()
                .filter(app -> "PENDING".equalsIgnoreCase(app.getStatus()))
                .count();

        long reviewed = applications.stream()
                .filter(app -> "REVIEWED".equalsIgnoreCase(app.getStatus()))
                .count();

        long accepted = applications.stream()
                .filter(app -> "ACCEPTED".equalsIgnoreCase(app.getStatus()))
                .count();

        long rejected = applications.stream()
                .filter(app -> "REJECTED".equalsIgnoreCase(app.getStatus()))
                .count();

        StringBuilder context = new StringBuilder();

        context.append("Thống kê ứng viên của chính công ty nhà tuyển dụng đang đăng nhập:\n")
                .append("- Tổng hồ sơ ứng tuyển: ").append(applications.size()).append("\n")
                .append("- Chờ xử lý: ").append(pending).append("\n")
                .append("- Đã xem: ").append(reviewed).append("\n")
                .append("- Được chọn: ").append(accepted).append("\n")
                .append("- Bị từ chối: ").append(rejected).append("\n")
                .append("Một số hồ sơ gần đây, email đã được ẩn bớt để bảo mật:\n");

        applications.stream()
                .limit(MAX_ITEMS)
                .forEach(app -> context.append("- Ứng viên: ")
                        .append(safe(app.getCandidateName()))
                        .append(" | Email: ")
                        .append(maskEmail(app.getCandidateEmail()))
                        .append(" | Job: ")
                        .append(app.getJob() != null ? safe(app.getJob().getTitle()) : "Không rõ")
                        .append(" | Trạng thái hồ sơ: ")
                        .append(toApplicationStatusText(app.getStatus()))
                        .append(" | Ngày ứng tuyển: ")
                        .append(app.getAppliedAt() != null ? app.getAppliedAt() : "Không rõ")
                        .append("\n"));

        return context.toString();
    }

    // =========================
    // ADMIN CONTEXT
    // =========================

    private String buildAdminDashboardContext() {
        return """
                Thống kê dành cho admin:
                - Tổng ứng viên: %d
                - Tổng nhà tuyển dụng: %d
                - Công ty đã duyệt: %d
                - Công ty chờ duyệt: %d
                - Tin đang tuyển công khai: %d
                - Tin chờ duyệt: %d
                - Tin hết hạn: %d
                - Tin bị khóa/từ chối: %d
                """.formatted(
                userRepository.countByRole("CANDIDATE"),
                userRepository.countByRole("EMPLOYER"),
                companyRepository.countByStatus("APPROVED"),
                companyRepository.countByStatus("PENDING"),
                jobRepository.countActivePublicJobs(),
                jobRepository.countByStatus("PENDING"),
                jobRepository.countByStatus("EXPIRED"),
                jobRepository.countByStatus("REJECTED")
        );
    }

    private String buildAdminPendingContext() {
        long pendingCompanies = companyRepository.countByStatus("PENDING");
        long pendingJobs = jobRepository.countByStatus("PENDING");

        return """
                Dữ liệu đang chờ admin duyệt:
                - Công ty chờ duyệt: %d
                - Tin tuyển dụng chờ duyệt: %d
                """.formatted(pendingCompanies, pendingJobs);
    }

    // =========================
    // AI PUBLIC JOB SEARCH
    // =========================

    private List<Job> findBestMatchingPublicJobs(String message) {
        String keyword = extractKeyword(message);

        if (!StringUtils.hasText(keyword)) {
            return jobRepository.findAiSearchablePublicJobs(
                    PageRequest.of(0, MAX_ITEMS)
            ).getContent();
        }

        List<Job> jobs = jobRepository.searchAiPublicJobsFullText(
                keyword,
                PageRequest.of(0, MAX_ITEMS)
        ).getContent();

        if (!jobs.isEmpty()) {
            return jobs;
        }

        return findBestMatchingPublicJobsFallback(message);
    }

    private List<Job> findBestMatchingPublicJobsFallback(String message) {
        String keyword = extractKeyword(message);
        List<String> tokens = extractSearchTokens(message);

        List<Job> candidates = jobRepository.findAiSearchablePublicJobs(
                PageRequest.of(0, MAX_SEARCH_CANDIDATES)
        ).getContent();

        if (candidates.isEmpty()) {
            return List.of();
        }

        if (tokens.isEmpty() && !StringUtils.hasText(keyword)) {
            return candidates.stream()
                    .limit(MAX_ITEMS)
                    .toList();
        }

        return candidates.stream()
                .map(job -> new ScoredJob(job, calculateJobSearchScore(job, keyword, tokens)))
                .filter(item -> item.score() > 0)
                .sorted((a, b) -> {
                    int scoreCompare = Integer.compare(b.score(), a.score());

                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }

                    if (a.job().getCreatedAt() == null && b.job().getCreatedAt() == null) {
                        return 0;
                    }

                    if (a.job().getCreatedAt() == null) {
                        return 1;
                    }

                    if (b.job().getCreatedAt() == null) {
                        return -1;
                    }

                    return b.job().getCreatedAt().compareTo(a.job().getCreatedAt());
                })
                .limit(MAX_ITEMS)
                .map(ScoredJob::job)
                .toList();
    }

    private int calculateJobSearchScore(Job job, String keyword, List<String> tokens) {
        String title = normalizeForSearch(job.getTitle());
        String description = normalizeForSearch(job.getDescription());
        String requirements = normalizeForSearch(job.getRequirements());
        String category = normalizeForSearch(job.getCategory());
        String location = normalizeForSearch(job.getLocation());

        String allText = String.join(" ", title, description, requirements, category, location);

        int score = 0;

        String normalizedKeyword = normalizeForSearch(keyword);

        if (StringUtils.hasText(normalizedKeyword) && allText.contains(normalizedKeyword)) {
            score += 20;
        }

        String compactAllText = allText.replace(" ", "");
        String compactKeyword = normalizedKeyword.replace(" ", "");

        if (StringUtils.hasText(compactKeyword) && compactAllText.contains(compactKeyword)) {
            score += 12;
        }

        for (String token : tokens) {
            if (title.contains(token)) {
                score += 8;
            }

            if (requirements.contains(token)) {
                score += 5;
            }

            if (description.contains(token)) {
                score += 3;
            }

            if (category.contains(token)) {
                score += 3;
            }

            if (location.contains(token)) {
                score += 1;
            }
        }

        if (tokens.size() >= 2) {
            long matchedTokens = tokens.stream()
                    .filter(allText::contains)
                    .count();

            if (matchedTokens == tokens.size()) {
                score += 10;
            }
        }

        return score;
    }

    // =========================
    // HELPER
    // =========================

    private User getUserByEmail(String email) {
        requireLogin(email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại!"));
    }

    private void requireLogin(String email) {
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("Bạn cần đăng nhập để sử dụng chức năng này!");
        }
    }

    private boolean isExpired(Job job) {
        if (job == null) {
            return false;
        }

        if ("EXPIRED".equalsIgnoreCase(job.getStatus())) {
            return true;
        }

        return job.getExpiredAt() != null
                && job.getExpiredAt().isBefore(LocalDate.now());
    }

    private boolean isClosed(Job job) {
        if (job == null) {
            return false;
        }

        return "CLOSED".equalsIgnoreCase(job.getStatus());
    }

    private String toJobStatusText(Job job) {
        if (job == null) {
            return "Không rõ";
        }

        if (isClosed(job)) {
            return "Đã đóng";
        }

        if (isExpired(job)) {
            return "Hết hạn";
        }

        String status = job.getStatus();

        if ("APPROVED".equalsIgnoreCase(status)) {
            return "Đang tuyển";
        }

        if ("PENDING".equalsIgnoreCase(status)) {
            return "Chờ duyệt";
        }

        if ("REJECTED".equalsIgnoreCase(status)) {
            return "Bị từ chối/khóa";
        }

        return safe(status);
    }

    private String toApplicationStatusText(String status) {
        if ("PENDING".equalsIgnoreCase(status)) {
            return "Chờ xử lý";
        }

        if ("REVIEWED".equalsIgnoreCase(status)) {
            return "Đã xem";
        }

        if ("ACCEPTED".equalsIgnoreCase(status)) {
            return "Được chọn";
        }

        if ("REJECTED".equalsIgnoreCase(status)) {
            return "Bị từ chối";
        }

        if ("WITHDRAWN".equalsIgnoreCase(status)) {
            return "Ứng viên đã rút hồ sơ";
        }

        return safe(status);
    }

    private void appendJobLine(StringBuilder context, Job job) {
        context.append("- ")
                .append(safe(job.getTitle()))
                .append(" | Công ty: ")
                .append(job.getCompany() != null ? safe(job.getCompany().getName()) : "Không rõ")
                .append(" | Địa điểm: ")
                .append(safe(job.getLocation()))
                .append(" | Ngành nghề: ")
                .append(safe(job.getCategory()))
                .append(" | Lương: ")
                .append(formatSalary(job.getSalary()))
                .append(" | Hạn: ")
                .append(formatDate(job.getExpiredAt()))
                .append("\n");
    }

    private String extractKeyword(String message) {
        List<String> tokens = extractSearchTokens(message);

        if (tokens.isEmpty()) {
            return "";
        }

        return String.join(" ", tokens);
    }

    private List<String> extractSearchTokens(String message) {
        String normalized = normalizeForSearch(message);

        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        return Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(token -> token.length() >= 2)
                .filter(token -> !SEARCH_STOP_WORDS.contains(token))
                .distinct()
                .limit(8)
                .toList();
    }

    private String normalizeForSearch(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("đ", "d");

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .replaceAll("[^a-z0-9+#.\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String formatSalary(Long salary) {
        if (salary == null || salary <= 0) {
            return "Thỏa thuận";
        }

        return String.format("%,d VNĐ", salary).replace(",", ".");
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "Không rõ";
        }

        return date.format(DATE_FORMATTER);
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "Chưa cập nhật";
    }

    private String safeToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "Không rõ";
        }

        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];

        if (name.length() <= 2) {
            return "***@" + domain;
        }

        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }

    private record ScoredJob(Job job, int score) {
    }
}