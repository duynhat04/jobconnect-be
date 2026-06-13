package com.jobconnect.service;

import com.jobconnect.dto.ApplicationCandidateDto;
import com.jobconnect.dto.ApplicationResponse;
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

    // Ứng viên nộp CV file mới
    @Transactional
    public ApplicationResponse applyWithNewCV(
            String userEmail,
            Long jobId,
            MultipartFile cvFile,
            String coverLetter) throws Exception {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findByIdWithCompany(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc này!"));

        if (job.getCompany() == null || job.getCompany().getUser() == null) {
            throw new RuntimeException("Tin tuyển dụng chưa liên kết với doanh nghiệp hợp lệ!");
        }

        if ("EXPIRED".equalsIgnoreCase(job.getStatus())) {
            throw new RuntimeException("Tin tuyển dụng đã hết hạn ứng tuyển!");
        }

        if (!"APPROVED".equalsIgnoreCase(job.getStatus())) {
            throw new RuntimeException("Tin tuyển dụng chưa được duyệt hoặc không khả dụng!");
        }

        if (job.getExpiredAt() != null && job.getExpiredAt().isBefore(java.time.LocalDate.now())) {
            job.setStatus("EXPIRED");
            jobRepository.save(job);

            throw new RuntimeException("Tin tuyển dụng đã hết hạn ứng tuyển!");
        }

        checkDuplicateApplication(user, job);

        if (cvFile == null || cvFile.isEmpty()) {
            throw new RuntimeException("Vui lòng tải lên CV!");
        }

        String cvUrl = cloudinaryStorageService.uploadCv(cvFile, user.getId());

        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setJob(job);
        application.setCvUrl(cvUrl);
        application.setCoverLetter(normalize(coverLetter));
        application.setStatus("PENDING");

        fillCandidateSnapshot(application, user);

        JobApplication savedApplication = applicationRepository.save(application);

        sendNewApplicationNotificationAsync(savedApplication, job, user, false);

        return toApplicationResponse(savedApplication);
    }

    // Ứng viên nộp bằng CV đã lưu
    @Transactional
    public ApplicationResponse applyWithExistingCV(
            String userEmail,
            Long jobId,
            Long userCvId,
            String coverLetter) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findByIdWithCompany(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc này!"));

        if (job.getCompany() == null || job.getCompany().getUser() == null) {
            throw new RuntimeException("Tin tuyển dụng chưa liên kết với doanh nghiệp hợp lệ!");
        }

        if ("EXPIRED".equalsIgnoreCase(job.getStatus())) {
            throw new RuntimeException("Tin tuyển dụng đã hết hạn ứng tuyển!");
        }

        if (!"APPROVED".equalsIgnoreCase(job.getStatus())) {
            throw new RuntimeException("Tin tuyển dụng chưa được duyệt hoặc không khả dụng!");
        }

        if (job.getExpiredAt() != null && job.getExpiredAt().isBefore(java.time.LocalDate.now())) {
            job.setStatus("EXPIRED");
            jobRepository.save(job);

            throw new RuntimeException("Tin tuyển dụng đã hết hạn ứng tuyển!");
        }

        checkDuplicateApplication(user, job);

        UserCV userCV = userCVRepository.findById(userCvId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV đã lưu!"));

        if (!userCV.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền sử dụng CV này!");
        }

        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setJob(job);
        application.setCvUrl(userCV.getFileUrl());
        application.setCoverLetter(normalize(coverLetter));
        application.setStatus("PENDING");

        fillCandidateSnapshot(application, user);

        JobApplication savedApplication = applicationRepository.save(application);

        sendNewApplicationNotificationAsync(savedApplication, job, user, true);

        return toApplicationResponse(savedApplication);
    }

    @Transactional
    public ApplicationResponse applyJob(
            String userEmail,
            Long jobId,
            MultipartFile cvFile,
            String coverLetter) throws Exception {
        return applyWithNewCV(userEmail, jobId, cvFile, coverLetter);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        return applicationRepository.findByUserIdOrderByAppliedAtDesc(user.getId())
                .stream()
                .map(this::toApplicationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForJob(Long jobId, String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findByIdWithCompany(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc!"));

        if (job.getCompany() == null || job.getCompany().getUser() == null) {
            throw new RuntimeException("Tin tuyển dụng chưa liên kết với doanh nghiệp hợp lệ!");
        }

        if (!job.getCompany().getUser().getId().equals(employer.getId())) {
            throw new RuntimeException("Bạn không có quyền xem CV của công ty khác!");
        }

        return applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId)
                .stream()
                .map(this::toApplicationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(
            Long applicationId,
            String newStatus,
            String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        JobApplication application = applicationRepository.findWithRelationsById(applicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ ứng tuyển!"));

        if (application.getJob() == null ||
                application.getJob().getCompany() == null ||
                application.getJob().getCompany().getUser() == null) {
            throw new RuntimeException("Hồ sơ ứng tuyển chưa liên kết với doanh nghiệp hợp lệ!");
        }

        if (!application.getJob().getCompany().getUser().getId().equals(employer.getId())) {
            throw new RuntimeException("Bạn không có quyền duyệt CV của công ty khác!");
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

        sendApplicationStatusNotificationAsync(savedApp, status);

        return toApplicationResponse(savedApp);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplicationsForMyCompany(String employerEmail) {
        return applicationRepository.findByJob_Company_User_EmailOrderByAppliedAtDesc(employerEmail)
                .stream()
                .map(this::toApplicationResponse)
                .collect(Collectors.toList());
    }

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

        Job job = jobRepository.findByIdWithCompany(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc!"));

        if (job.getCompany() == null || job.getCompany().getUser() == null) {
            throw new RuntimeException("Tin tuyển dụng chưa liên kết với doanh nghiệp hợp lệ!");
        }

        if (!job.getCompany().getUser().getId().equals(employer.getId())) {
            throw new RuntimeException("Bạn không có quyền xem ứng viên của tin tuyển dụng này!");
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

    private void fillCandidateSnapshot(JobApplication application, User user) {
        application.setCandidateName(user.getFullName());
        application.setCandidateEmail(user.getEmail());
        application.setCandidatePhone(user.getPhone());
        application.setCandidateAddress(user.getAddress());
        application.setCandidateSkills(user.getSkills());
        application.setCandidateDesiredPosition(user.getDesiredPosition());
        application.setCandidateDesiredCategory(user.getDesiredCategory());
        application.setCandidateExperienceYears(user.getExperienceYears());
        application.setCandidateExpectedSalary(user.getExpectedSalary());
        application.setCandidateWorkType(user.getWorkType());
        application.setCandidateEducationLevel(user.getEducationLevel());
        application.setCandidateEnglishLevel(user.getEnglishLevel());
        application.setCandidateCertificates(user.getCertificates());
        application.setCandidateProjects(user.getProjects());
        application.setCandidateAvailableFrom(user.getAvailableFrom());
        application.setCandidatePortfolioUrl(user.getPortfolioUrl());
        application.setCandidateLinkedinUrl(user.getLinkedinUrl());
    }

    private ApplicationResponse toApplicationResponse(JobApplication app) {
        User user = app.getUser();
        Job job = app.getJob();

        Long companyId = null;
        String companyName = null;
        String companyLogo = null;
        String companyAddress = null;

        if (job != null && job.getCompany() != null) {
            companyId = job.getCompany().getId();
            companyName = job.getCompany().getName();
            companyLogo = job.getCompany().getLogo();
            companyAddress = job.getCompany().getAddress();
        }

        return ApplicationResponse.builder()
                .id(app.getId())

                // =========================
                // JOB INFO
                // =========================
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .jobCategory(job != null ? job.getCategory() : null)
                .jobLocation(job != null ? job.getLocation() : null)
                .jobSalary(job != null ? job.getSalary() : null)
                .employmentType(
                        job != null && job.getEmploymentType() != null
                                ? job.getEmploymentType().name()
                                : null)
                .jobExpiredAt(job != null ? job.getExpiredAt() : null)
                .jobStatus(job != null ? job.getStatus() : null)

                // =========================
                // COMPANY INFO
                // =========================
                .companyId(companyId)
                .companyName(companyName)
                .companyLogo(companyLogo)
                .companyAddress(companyAddress)

                // =========================
                // CANDIDATE SNAPSHOT
                // =========================
                .candidateId(user != null ? user.getId() : null)
                .candidateName(firstNotBlank(app.getCandidateName(), user != null ? user.getFullName() : null))
                .candidateEmail(firstNotBlank(app.getCandidateEmail(), user != null ? user.getEmail() : null))
                .candidatePhone(firstNotBlank(app.getCandidatePhone(), user != null ? user.getPhone() : null))
                .candidateAddress(firstNotBlank(app.getCandidateAddress(), user != null ? user.getAddress() : null))

                .candidateSkills(firstNotBlank(app.getCandidateSkills(), user != null ? user.getSkills() : null))
                .candidateDesiredPosition(
                        firstNotBlank(app.getCandidateDesiredPosition(),
                                user != null ? user.getDesiredPosition() : null))
                .candidateDesiredCategory(
                        firstNotBlank(app.getCandidateDesiredCategory(),
                                user != null ? user.getDesiredCategory() : null))
                .candidateExperienceYears(
                        app.getCandidateExperienceYears() != null
                                ? app.getCandidateExperienceYears()
                                : user != null ? user.getExperienceYears() : null)
                .candidateExpectedSalary(
                        app.getCandidateExpectedSalary() != null
                                ? app.getCandidateExpectedSalary()
                                : user != null ? user.getExpectedSalary() : null)
                .candidateWorkType(firstNotBlank(app.getCandidateWorkType(), user != null ? user.getWorkType() : null))
                .candidateEducationLevel(
                        firstNotBlank(app.getCandidateEducationLevel(), user != null ? user.getEducationLevel() : null))
                .candidateEnglishLevel(
                        firstNotBlank(app.getCandidateEnglishLevel(), user != null ? user.getEnglishLevel() : null))
                .candidateCertificates(
                        firstNotBlank(app.getCandidateCertificates(), user != null ? user.getCertificates() : null))
                .candidateProjects(
                        firstNotBlank(app.getCandidateProjects(), user != null ? user.getProjects() : null))
                .candidateAvailableFrom(
                        firstNotBlank(app.getCandidateAvailableFrom(), user != null ? user.getAvailableFrom() : null))
                .candidatePortfolioUrl(
                        firstNotBlank(app.getCandidatePortfolioUrl(), user != null ? user.getPortfolioUrl() : null))
                .candidateLinkedinUrl(
                        firstNotBlank(app.getCandidateLinkedinUrl(), user != null ? user.getLinkedinUrl() : null))

                // =========================
                // APPLICATION INFO
                // =========================
                .cvUrl(app.getCvUrl())
                .coverLetter(app.getCoverLetter())
                .status(app.getStatus())
                .appliedAt(app.getAppliedAt())
                .build();
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
                firstNotBlank(app.getCandidateName(), user.getFullName()),
                firstNotBlank(app.getCandidateEmail(), user.getEmail()),
                firstNotBlank(app.getCandidatePhone(), user.getPhone()),
                firstNotBlank(app.getCandidateAddress(), user.getAddress()),

                user.getBio(),
                firstNotBlank(app.getCandidateSkills(), user.getSkills()),
                app.getCvUrl(),
                app.getCoverLetter(),

                firstNotBlank(app.getCandidateDesiredPosition(), user.getDesiredPosition()),
                firstNotBlank(app.getCandidateDesiredCategory(), user.getDesiredCategory()),
                app.getCandidateExperienceYears() != null ? app.getCandidateExperienceYears()
                        : user.getExperienceYears(),
                app.getCandidateExpectedSalary() != null ? app.getCandidateExpectedSalary() : user.getExpectedSalary(),
                firstNotBlank(app.getCandidateWorkType(), user.getWorkType()),
                firstNotBlank(app.getCandidateEducationLevel(), user.getEducationLevel()),
                firstNotBlank(app.getCandidateEnglishLevel(), user.getEnglishLevel()),
                firstNotBlank(app.getCandidateCertificates(), user.getCertificates()),
                firstNotBlank(app.getCandidateProjects(), user.getProjects()),
                firstNotBlank(app.getCandidateAvailableFrom(), user.getAvailableFrom()),

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

        String userSkills = safe(firstNotBlank(app.getCandidateSkills(), user.getSkills()));
        String desiredPosition = safe(firstNotBlank(app.getCandidateDesiredPosition(), user.getDesiredPosition()));
        String desiredCategory = safe(firstNotBlank(app.getCandidateDesiredCategory(), user.getDesiredCategory()));
        String userAddress = safe(firstNotBlank(app.getCandidateAddress(), user.getAddress()));
        String bio = safe(user.getBio());
        String projects = safe(firstNotBlank(app.getCandidateProjects(), user.getProjects()));
        String certificates = safe(firstNotBlank(app.getCandidateCertificates(), user.getCertificates()));
        String coverLetter = safe(app.getCoverLetter());

        if (notBlank(app.getCvUrl())) {
            score += 10;
        }

        if (textRelated(jobTitle, desiredPosition)) {
            score += 20;
        }

        if (textRelated(jobCategory, desiredCategory)) {
            score += 15;
        }

        int skillScore = calculateKeywordOverlapScore(userSkills, jobRequirements);
        score += Math.min(skillScore, 35);

        int profileScore = 0;
        profileScore += calculateKeywordOverlapScore(bio, jobRequirements);
        profileScore += calculateKeywordOverlapScore(projects, jobRequirements);
        profileScore += calculateKeywordOverlapScore(certificates, jobRequirements);
        profileScore += calculateKeywordOverlapScore(userSkills, jobDescription);
        profileScore += calculateKeywordOverlapScore(coverLetter, jobRequirements);
        score += Math.min(profileScore, 20);

        Integer experienceYears = app.getCandidateExperienceYears() != null
                ? app.getCandidateExperienceYears()
                : user.getExperienceYears();

        if (experienceYears != null) {
            if (experienceYears >= 3) {
                score += 15;
            } else if (experienceYears >= 1) {
                score += 10;
            } else if (experienceYears == 0) {
                score += 3;
            }
        }

        if (textRelated(jobLocation, userAddress)) {
            score += 10;
        }

        Long expectedSalary = app.getCandidateExpectedSalary() != null
                ? app.getCandidateExpectedSalary()
                : user.getExpectedSalary();

        if (job.getSalary() != null) {
            if (expectedSalary == null) {
                score += 5;
            } else if (expectedSalary <= job.getSalary()) {
                score += 10;
            }
        }

        if (notBlank(firstNotBlank(app.getCandidateEducationLevel(), user.getEducationLevel()))) {
            score += 3;
        }

        if (notBlank(firstNotBlank(app.getCandidateEnglishLevel(), user.getEnglishLevel()))) {
            score += 3;
        }

        if (notBlank(firstNotBlank(app.getCandidateProjects(), user.getProjects()))) {
            score += 5;
        }

        if (notBlank(firstNotBlank(app.getCandidateCertificates(), user.getCertificates()))) {
            score += 4;
        }

        return Math.min(score, 100);
    }

    private void sendNewApplicationNotificationAsync(
            JobApplication application,
            Job job,
            User user,
            boolean existingCv) {
        Long employerUserId = job.getCompany().getUser().getId();
        String candidateName = firstNotBlank(application.getCandidateName(), user.getFullName());
        String jobTitle = job.getTitle();

        CompletableFuture.runAsync(() -> {
            try {
                String message = existingCv
                        ? "Ứng viên " + candidateName + " vừa sử dụng CV đã lưu để nộp vào vị trí " + jobTitle
                        : "Ứng viên " + candidateName + " vừa nộp CV vào vị trí " + jobTitle;

                notificationService.createNotification(
                        employerUserId,
                        "Có ứng tuyển mới!",
                        message,
                        "NEW_APPLICATION",
                        "/employer/manage-applications");
            } catch (Exception e) {
                System.err.println("Lỗi bắn thông báo ngầm: " + e.getMessage());
            }
        });
    }

    private void sendApplicationStatusNotificationAsync(JobApplication savedApp, String finalStatus) {
        Long candidateUserId = savedApp.getUser().getId();
        String candidateEmail = firstNotBlank(savedApp.getCandidateEmail(), savedApp.getUser().getEmail());
        String jobTitle = savedApp.getJob().getTitle();
        String companyName = savedApp.getJob().getCompany().getName();

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
                System.err.println("Lỗi bắn thông báo ngầm: " + e.getMessage());
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
                System.err.println("Lỗi gửi email ngầm: " + e.getMessage());
            }
        });
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

    private String firstNotBlank(String first, String second) {
        return notBlank(first) ? first : second;
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