package com.jobconnect.service;

import com.cloudinary.Cloudinary;
import com.jobconnect.dto.ApplicationCandidateDto;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import com.jobconnect.entity.UserCV;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserCVRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class JobApplicationService {

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCVRepository userCVRepository;

    @Autowired
    private CloudinaryStorageService cloudinaryStorageService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    private void checkDuplicateApplication(User user, Job job) {
        if (applicationRepository.existsByUserAndJob(user, job)) {
            throw new RuntimeException(
                    "Bạn đã ứng tuyển vào công việc này rồi! Vui lòng chờ phản hồi từ nhà tuyển dụng.");
        }
    }

    // LUỒNG 1: Ứng viên nộp CV bằng file mới upload
    @Transactional
    public JobApplication applyWithNewCV(String userEmail, Long jobId, MultipartFile cvFile, String coverLetter)
            throws Exception {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc này!"));

        if (job.getCompany() == null || job.getCompany().getUser() == null) {
            throw new RuntimeException("Tin tuyển dụng chưa liên kết với doanh nghiệp hợp lệ!");
        }

        checkDuplicateApplication(user, job);

        String cvUrl = cloudinaryStorageService.uploadCv(cvFile, user.getId());

        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setJob(job);
        application.setCvUrl(cvUrl);
        application.setCoverLetter(coverLetter);
        application.setStatus("PENDING");

        JobApplication savedApplication = applicationRepository.save(application);

        Long employerUserId = job.getCompany().getUser().getId();
        String candidateName = user.getFullName();
        String jobTitle = job.getTitle();

        CompletableFuture.runAsync(() -> {
            try {
                notificationService.createNotification(
                        employerUserId,
                        "Có ứng tuyển mới!",
                        "Ứng viên " + candidateName + " vừa nộp CV vào vị trí " + jobTitle,
                        "NEW_APPLICATION",
                        "/employer/manage-applications");
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi bắn thông báo ngầm: " + e.getMessage());
            }
        });

        return savedApplication;
    }

    // LUỒNG 2: Ứng viên nộp bằng CV đã lưu trong hệ thống
    @Transactional
    public JobApplication applyWithExistingCV(String userEmail, Long jobId, Long userCvId, String coverLetter) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc này!"));

        checkDuplicateApplication(user, job);

        UserCV userCV = userCVRepository.findById(userCvId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV đã lưu!"));

        if (!userCV.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Lỗi bảo mật: Bạn không có quyền sử dụng CV này!");
        }

        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setJob(job);
        application.setCvUrl(userCV.getFileUrl());
        application.setCoverLetter(coverLetter);
        application.setStatus("PENDING");

        JobApplication savedApplication = applicationRepository.save(application);

        Long employerUserId = job.getCompany().getUser().getId();
        String candidateName = user.getFullName();
        String jobTitle = job.getTitle();

        CompletableFuture.runAsync(() -> {
            try {
                notificationService.createNotification(
                        employerUserId,
                        "Có ứng tuyển mới!",
                        "Ứng viên " + candidateName + " vừa sử dụng CV đã lưu để nộp vào vị trí " + jobTitle,
                        "NEW_APPLICATION",
                        "/employer/manage-applications");
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi bắn thông báo ngầm: " + e.getMessage());
            }
        });

        return savedApplication;
    }

    // LUỒNG 3: API upload CV
    @Transactional
    public JobApplication applyJob(String userEmail, Long jobId, MultipartFile cvFile, String coverLetter)
            throws Exception {
        return applyWithNewCV(userEmail, jobId, cvFile, coverLetter);
    }

    // Ứng viên xem danh sách công việc đã ứng tuyển
    @Transactional(readOnly = true)
    public List<JobApplication> getMyApplications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        return applicationRepository.findByUserId(user.getId());
    }

    // API cũ: NTD xem danh sách hồ sơ của 1 job
    @Transactional(readOnly = true)
    public List<JobApplication> getApplicationsForJob(Long jobId, String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc!"));

        if (job.getCompany() == null || job.getCompany().getUser() == null) {
            throw new RuntimeException("Tin tuyển dụng chưa liên kết với doanh nghiệp hợp lệ!");
        }

        if (!job.getCompany().getUser().getId().equals(employer.getId())) {
            throw new RuntimeException("LỖI BẢO MẬT: Bạn không có quyền xem CV của công ty khác!");
        }

        return applicationRepository.findByJobId(jobId);
    }

    // NTD cập nhật trạng thái hồ sơ ứng tuyển
    @Transactional
    public JobApplication updateApplicationStatus(Long applicationId, String newStatus, String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ ứng tuyển!"));

        if (application.getJob() == null ||
                application.getJob().getCompany() == null ||
                application.getJob().getCompany().getUser() == null) {
            throw new RuntimeException("Hồ sơ ứng tuyển chưa liên kết với doanh nghiệp hợp lệ!");
        }

        if (!application.getJob().getCompany().getUser().getId().equals(employer.getId())) {
            throw new RuntimeException("LỖI BẢO MẬT: Bạn không có quyền duyệt CV của công ty khác!");
        }

        String status = normalize(newStatus);
        if (status == null) {
            throw new RuntimeException("Trạng thái hồ sơ không hợp lệ!");
        }

        status = status.toUpperCase();

        Set<String> allowedStatuses = Set.of("PENDING", "REVIEWED", "ACCEPTED", "REJECTED");
        if (!allowedStatuses.contains(status)) {
            throw new RuntimeException("Trạng thái hồ sơ không được hỗ trợ!");
        }

        application.setStatus(status);
        JobApplication savedApp = applicationRepository.save(application);

        Long candidateUserId = savedApp.getUser().getId();
        String candidateEmail = savedApp.getUser().getEmail();
        String jobTitle = savedApp.getJob().getTitle();
        String companyName = savedApp.getJob().getCompany().getName();
        String finalStatus = status;

        CompletableFuture.runAsync(() -> {
            try {
                String statusVN = "ACCEPTED".equalsIgnoreCase(finalStatus) ? "được CHẤP NHẬN" : "bị TỪ CHỐI";

                if ("ACCEPTED".equalsIgnoreCase(finalStatus) || "REJECTED".equalsIgnoreCase(finalStatus)) {
                    notificationService.createNotification(
                            candidateUserId,
                            "Cập nhật trạng thái ứng tuyển",
                            "Hồ sơ của bạn tại vị trí " + jobTitle + " đã " + statusVN,
                            "APPLICATION_STATUS",
                            "/candidate/my-applications");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi bắn thông báo ngầm: " + e.getMessage());
            }

            try {
                if ("ACCEPTED".equalsIgnoreCase(finalStatus)) {
                    String subject = "🎉 Chúc mừng! Hồ sơ của bạn đã được duyệt";
                    String body = "Chào bạn,\n\nCông ty " + companyName + " đã xem xét hồ sơ của bạn cho vị trí "
                            + jobTitle + ".\n"
                            + "Chúng tôi rất ấn tượng và muốn mời bạn tham gia vòng phỏng vấn tiếp theo.\n\n"
                            + "Bộ phận nhân sự sẽ sớm liên hệ với bạn để chốt lịch hẹn.\n\n"
                            + "Trân trọng,\n" + companyName;

                    emailService.sendEmail(candidateEmail, subject, body);
                } else if ("REJECTED".equalsIgnoreCase(finalStatus)) {
                    String subject = "Thông báo kết quả ứng tuyển vị trí " + jobTitle;
                    String body = "Chào bạn,\n\nCảm ơn bạn đã quan tâm và ứng tuyển vào vị trí " + jobTitle + " tại "
                            + companyName + ".\n"
                            + "Rất tiếc, hồ sơ của bạn chưa phù hợp với tiêu chí của chúng tôi ở thời điểm hiện tại.\n\n"
                            + "Chúc bạn may mắn và thành công trên con đường sự nghiệp!\n\n"
                            + "Trân trọng,\n" + companyName;

                    emailService.sendEmail(candidateEmail, subject, body);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi gửi email ngầm: " + e.getMessage());
            }
        });

        return savedApp;
    }

    // API NTD xem tất cả hồ sơ ứng tuyển của công ty
    @Transactional(readOnly = true)
    public List<JobApplication> getAllApplicationsForMyCompany(String employerEmail) {
        return applicationRepository.findByJob_Company_User_EmailOrderByIdDesc(employerEmail);
    }

    // API NTD xem ứng viên theo từng job, tự chấm điểm phù hợp với yêu cầu job
    @Transactional(readOnly = true)
    public Page<ApplicationCandidateDto> searchCandidatesForJob(
            Long jobId,
            String employerEmail,
            String keyword,
            String status,
            int page,
            int size) {
        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 10;
        }

        if (size > 50) {
            size = 50;
        }

        User employer = userRepository.findByEmail(employerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà tuyển dụng!"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc!"));

        if (job.getCompany() == null || job.getCompany().getUser() == null) {
            throw new RuntimeException("Tin tuyển dụng chưa liên kết với doanh nghiệp hợp lệ!");
        }

        if (!job.getCompany().getUser().getId().equals(employer.getId())) {
            throw new RuntimeException("LỖI BẢO MẬT: Bạn không có quyền xem ứng viên của tin tuyển dụng này!");
        }

        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalize(status);

        List<JobApplication> applications = applicationRepository.findCandidatesForJobWithFilter(
                jobId,
                employerEmail,
                normalizedKeyword,
                normalizedStatus);

        List<ApplicationCandidateDto> result = applications.stream()
                .map(this::toApplicationCandidateDto)
                .sorted(
                        Comparator.comparing(
                                ApplicationCandidateDto::getMatchScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(
                                        ApplicationCandidateDto::getAppliedAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int start = page * size;
        int end = Math.min(start + size, result.size());

        List<ApplicationCandidateDto> pageContent = start >= result.size() ? List.of() : result.subList(start, end);

        return new PageImpl<>(
                pageContent,
                PageRequest.of(page, size),
                result.size());
    }

    private ApplicationCandidateDto toApplicationCandidateDto(JobApplication app) {
        User user = app.getUser();
        Job job = app.getJob();

        int score = calculateJobBasedMatchScore(user, job, app);

        return new ApplicationCandidateDto(
                app.getId(),
                job.getId(),
                job.getTitle(),

                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),

                user.getBio(),
                user.getSkills(),
                app.getCvUrl(),
                app.getCoverLetter(),

                user.getDesiredPosition(),
                user.getDesiredCategory(),
                user.getExperienceYears(),
                user.getExpectedSalary(),
                user.getWorkType(),
                user.getEducationLevel(),
                user.getEnglishLevel(),
                user.getCertificates(),
                user.getProjects(),
                user.getAvailableFrom(),

                app.getStatus(),
                app.getAppliedAt(),

                score);
    }

    private int calculateJobBasedMatchScore(User user, Job job, JobApplication app) {
        int score = 0;

        String jobTitle = safe(job.getTitle());
        String jobCategory = safe(job.getCategory());
        String jobRequirements = safe(job.getRequirements());
        String jobDescription = safe(job.getDescription());
        String jobLocation = safe(job.getLocation());

        String userSkills = safe(user.getSkills());
        String desiredPosition = safe(user.getDesiredPosition());
        String desiredCategory = safe(user.getDesiredCategory());
        String userAddress = safe(user.getAddress());
        String bio = safe(user.getBio());
        String projects = safe(user.getProjects());
        String certificates = safe(user.getCertificates());
        String coverLetter = safe(app.getCoverLetter());

        // 1. Có CV trong hồ sơ ứng tuyển
        if (notBlank(app.getCvUrl())) {
            score += 10;
        }

        // 2. Vị trí mong muốn khớp tên job
        if (textRelated(jobTitle, desiredPosition)) {
            score += 20;
        }

        // 3. Ngành nghề mong muốn khớp category job
        if (textRelated(jobCategory, desiredCategory)) {
            score += 15;
        }

        // 4. Kỹ năng khớp yêu cầu công việc
        int skillScore = calculateKeywordOverlapScore(userSkills, jobRequirements);
        score += Math.min(skillScore, 35);

        // 5. Thông tin hồ sơ khớp mô tả/yêu cầu công việc
        int profileScore = 0;
        profileScore += calculateKeywordOverlapScore(bio, jobRequirements);
        profileScore += calculateKeywordOverlapScore(projects, jobRequirements);
        profileScore += calculateKeywordOverlapScore(certificates, jobRequirements);
        profileScore += calculateKeywordOverlapScore(userSkills, jobDescription);
        profileScore += calculateKeywordOverlapScore(coverLetter, jobRequirements);
        score += Math.min(profileScore, 20);

        // 6. Kinh nghiệm
        if (user.getExperienceYears() != null) {
            if (user.getExperienceYears() >= 3) {
                score += 15;
            } else if (user.getExperienceYears() >= 1) {
                score += 10;
            } else if (user.getExperienceYears() == 0) {
                score += 3;
            }
        }

        // 7. Địa điểm
        if (textRelated(jobLocation, userAddress)) {
            score += 10;
        }

        // 8. Lương mong muốn
        // Nếu ứng viên không nhập lương thì không loại hồ sơ.
        if (job.getSalary() != null) {
            if (user.getExpectedSalary() == null) {
                score += 5;
            } else if (user.getExpectedSalary() <= job.getSalary()) {
                score += 10;
            }
        }

        // 9. Hồ sơ đầy đủ hơn được cộng điểm nhẹ
        if (notBlank(user.getEducationLevel())) {
            score += 3;
        }

        if (notBlank(user.getEnglishLevel())) {
            score += 3;
        }

        if (notBlank(user.getProjects())) {
            score += 5;
        }

        if (notBlank(user.getCertificates())) {
            score += 4;
        }

        return Math.min(score, 100);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean textRelated(String a, String b) {
        if (!notBlank(a) || !notBlank(b)) {
            return false;
        }

        String left = a.toLowerCase();
        String right = b.toLowerCase();

        return left.contains(right) || right.contains(left) || hasCommonKeyword(left, right);
    }

    private boolean hasCommonKeyword(String a, String b) {
        Set<String> wordsA = extractKeywords(a);
        Set<String> wordsB = extractKeywords(b);

        for (String word : wordsA) {
            if (wordsB.contains(word)) {
                return true;
            }
        }

        return false;
    }

    private int calculateKeywordOverlapScore(String candidateText, String jobText) {
        if (!notBlank(candidateText) || !notBlank(jobText)) {
            return 0;
        }

        Set<String> candidateWords = extractKeywords(candidateText);
        Set<String> jobWords = extractKeywords(jobText);

        if (candidateWords.isEmpty() || jobWords.isEmpty()) {
            return 0;
        }

        int matchCount = 0;

        for (String word : candidateWords) {
            if (jobWords.contains(word)) {
                matchCount++;
            }
        }

        return matchCount * 5;
    }

    private Set<String> extractKeywords(String text) {
        Set<String> result = new HashSet<>();

        if (!notBlank(text)) {
            return result;
        }

        String normalized = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9À-ỹ\\s+#.]", " ");

        String[] words = normalized.split("\\s+");

        for (String word : words) {
            String w = word.trim();

            if (w.length() < 2) {
                continue;
            }

            if (isStopWord(w)) {
                continue;
            }

            result.add(w);
        }

        return result;
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
                "và", "hoặc", "là", "của", "cho", "với", "các", "một", "những",
                "the", "and", "or", "to", "of", "in", "on", "for", "a", "an",
                "kinh", "nghiệm", "yêu", "cầu", "mô", "tả", "công", "việc",
                "ứng", "viên", "làm");

        return stopWords.contains(word);
    }
}