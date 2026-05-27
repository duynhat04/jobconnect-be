package com.jobconnect.service;

import com.cloudinary.Cloudinary;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import com.jobconnect.entity.UserCV;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserCVRepository;
import com.jobconnect.repository.UserRepository;
import com.jobconnect.dto.ApplicationCandidateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    private Cloudinary cloudinary;

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

    // LUỒNG 1: TẢI CV MỚI LÊN
    @Transactional
    public JobApplication applyWithNewCV(String userEmail, Long jobId, MultipartFile cvFile, String coverLetter)
            throws Exception {
        // BẢO MẬT HIỆU NĂNG: Chặn file rác, quá dung lượng (Ví dụ giới hạn 5MB)
        if (cvFile.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Kích thước file CV quá lớn. Vui lòng upload file dưới 5MB!");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc này!"));

        checkDuplicateApplication(user, job);

        // Đẩy lên Cloudinary
        Map<String, Object> uploadOptions = new java.util.HashMap<>();
        uploadOptions.put("resource_type", "auto");
        Map uploadResult = cloudinary.uploader().upload(cvFile.getBytes(), uploadOptions);

        // Tránh lỗi NullPointerException nếu Cloudinary trả về thiếu secure_url
        if (uploadResult.get("secure_url") == null) {
            throw new RuntimeException("Lỗi upload CV lên server đám mây!");
        }
        String cvUrl = uploadResult.get("secure_url").toString();

        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setJob(job);
        application.setCvUrl(cvUrl);
        application.setCoverLetter(coverLetter);

        JobApplication savedApplication = applicationRepository.save(application);

        // CHUẨN DEPLOY: Bắn thông báo ngầm (Bất đồng bộ) để API trả kết quả ngay lập
        // tức
        CompletableFuture.runAsync(() -> {
            try {
                notificationService.createNotification(
                        savedApplication.getJob().getCompany().getUser().getId(),
                        "Có ứng tuyển mới!",
                        "Ứng viên " + savedApplication.getUser().getFullName() + " vừa nộp CV vào vị trí "
                                + savedApplication.getJob().getTitle(),
                        "NEW_APPLICATION",
                        "/employer/manage-applications");
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi bắn thông báo ngầm: " + e.getMessage());
            }
        });

        return savedApplication;
    }

    // LUỒNG 2: DÙNG CV ĐÃ LƯU TRONG HỆ THỐNG
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

        JobApplication savedApplication = applicationRepository.save(application);

        // Bắn thông báo ngầm
        CompletableFuture.runAsync(() -> {
            try {
                notificationService.createNotification(
                        savedApplication.getJob().getCompany().getUser().getId(),
                        "Có ứng tuyển mới!",
                        "Ứng viên " + savedApplication.getUser().getFullName()
                                + " vừa sử dụng CV đã lưu để nộp vào vị trí " + savedApplication.getJob().getTitle(),
                        "NEW_APPLICATION",
                        "/employer/manage-applications");
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi bắn thông báo ngầm: " + e.getMessage());
            }
        });

        return savedApplication;
    }

    // LUỒNG 3: NỘP CV CHUNG (Thực ra giống hệt Luồng 1, nhưng tớ vẫn tối ưu lại cho
    // an toàn)
    @Transactional
    public JobApplication applyJob(String userEmail, Long jobId, MultipartFile cvFile, String coverLetter)
            throws Exception {
        return applyWithNewCV(userEmail, jobId, cvFile, coverLetter); // Tái sử dụng code Luồng 1 cho Clean Code
    }

    @Transactional(readOnly = true)
    public List<JobApplication> getMyApplications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        return applicationRepository.findByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public List<JobApplication> getApplicationsForJob(Long jobId, String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc!"));

        if (!job.getCompany().getUser().getId().equals(employer.getId())) {
            throw new RuntimeException("LỖI BẢO MẬT: Bạn không có quyền xem CV của công ty khác!");
        }
        return applicationRepository.findByJobId(jobId);
    }

    // 3. Nhà tuyển dụng đổi trạng thái CV và Gửi Email tự động
    @Transactional
    public JobApplication updateApplicationStatus(Long applicationId, String newStatus, String employerEmail) {
        User employer = userRepository.findByEmail(employerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ ứng tuyển!"));

        if (!application.getJob().getCompany().getUser().getId().equals(employer.getId())) {
            throw new RuntimeException("LỖI BẢO MẬT: Bạn không có quyền duyệt CV của công ty khác!");
        }

        application.setStatus(newStatus.toUpperCase());
        JobApplication savedApp = applicationRepository.save(application);

        // CHUẨN DEPLOY: Đẩy việc gửi Email và Thông báo ra luồng ngầm (Background Task)
        CompletableFuture.runAsync(() -> {
            // 1. Bắn thông báo
            try {
                String statusVN = "ACCEPTED".equalsIgnoreCase(newStatus) ? "được CHẤP NHẬN" : "bị TỪ CHỐI";
                notificationService.createNotification(
                        savedApp.getUser().getId(),
                        "Cập nhật trạng thái ứng tuyển",
                        "Hồ sơ của bạn tại vị trí " + savedApp.getJob().getTitle() + " đã " + statusVN,
                        "APPLICATION_STATUS",
                        "/candidate/my-applications");
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi bắn thông báo ngầm: " + e.getMessage());
            }

            // 2. Bắn Email
            try {
                String candidateEmail = savedApp.getUser().getEmail();
                String jobTitle = savedApp.getJob().getTitle();
                String companyName = savedApp.getJob().getCompany().getName();

                if ("ACCEPTED".equalsIgnoreCase(newStatus)) {
                    String subject = "🎉 Chúc mừng! Hồ sơ của bạn đã được duyệt";
                    String body = "Chào bạn,\n\nCông ty " + companyName + " đã xem xét hồ sơ của bạn cho vị trí "
                            + jobTitle + ".\n" +
                            "Chúng tôi rất ấn tượng và muốn mời bạn tham gia vòng phỏng vấn tiếp theo.\n\nBộ phận nhân sự sẽ sớm liên hệ với bạn để chốt lịch hẹn.\n\nTrân trọng,\n"
                            + companyName;
                    emailService.sendEmail(candidateEmail, subject, body);
                } else if ("REJECTED".equalsIgnoreCase(newStatus)) {
                    String subject = "Thông báo kết quả ứng tuyển vị trí " + jobTitle;
                    String body = "Chào bạn,\n\nCảm ơn bạn đã quan tâm và ứng tuyển vào vị trí " + jobTitle + " tại "
                            + companyName + ".\n" +
                            "Rất tiếc, hồ sơ của bạn chưa phù hợp với tiêu chí của chúng tôi ở thời điểm hiện tại.\n\nChúc bạn may mắn và thành công trên con đường sự nghiệp!\n\nTrân trọng,\n"
                            + companyName;
                    emailService.sendEmail(candidateEmail, subject, body);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi khi gửi email ngầm: " + e.getMessage());
            }
        });

        return savedApp;
    }

    @Transactional(readOnly = true)
    public List<JobApplication> getAllApplicationsForMyCompany(String employerEmail) {
        return applicationRepository.findByJob_Company_User_EmailOrderByIdDesc(employerEmail);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationCandidateDto> searchCandidatesForJob(
            Long jobId,
            String employerEmail,
            String keyword,
            String skill,
            Integer minExperience,
            Long maxExpectedSalary,
            String workType,
            String educationLevel,
            String englishLevel,
            String status,
            int page,
            int size) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;
        }
        // Chặn page size quá lớn để tránh nặng server khi deploy
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
        String normalizedSkill = normalize(skill);
        String normalizedWorkType = normalize(workType);
        String normalizedEducationLevel = normalize(educationLevel);
        String normalizedEnglishLevel = normalize(englishLevel);
        String normalizedStatus = normalize(status);
        List<JobApplication> applications = applicationRepository.searchCandidatesForJob(
                jobId,
                employerEmail,
                normalizedKeyword,
                normalizedSkill,
                minExperience,
                maxExpectedSalary,
                normalizedWorkType,
                normalizedEducationLevel,
                normalizedEnglishLevel,
                normalizedStatus);
        List<ApplicationCandidateDto> result = applications.stream()
                .map(app -> toApplicationCandidateDto(
                        app,
                        normalizedSkill,
                        minExperience,
                        maxExpectedSalary,
                        normalizedWorkType,
                        normalizedEducationLevel,
                        normalizedEnglishLevel))
                .sorted(
                        Comparator
                                .comparing(ApplicationCandidateDto::getMatchScore,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(ApplicationCandidateDto::getAppliedAt,
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

    private ApplicationCandidateDto toApplicationCandidateDto(
            JobApplication app,
            String skill,
            Integer minExperience,
            Long maxExpectedSalary,
            String workType,
            String educationLevel,
            String englishLevel) {
        User user = app.getUser();
        Job job = app.getJob();

        int score = calculateMatchScore(
                user,
                job,
                skill,
                minExperience,
                maxExpectedSalary,
                workType,
                educationLevel,
                englishLevel);

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

    private int calculateMatchScore(
            User user,
            Job job,
            String skill,
            Integer minExperience,
            Long maxExpectedSalary,
            String workType,
            String educationLevel,
            String englishLevel) {
        int score = 0;

        if (notBlank(user.getCvUrl())) {
            score += 10;
        }

        if (notBlank(user.getBio())) {
            score += 5;
        }

        if (notBlank(user.getSkills())) {
            score += 10;
        }

        if (notBlank(skill) && containsIgnoreCase(user.getSkills(), skill)) {
            score += 35;
        }

        if (minExperience != null && user.getExperienceYears() != null
                && user.getExperienceYears() >= minExperience) {
            score += 20;
        }

        if (maxExpectedSalary != null) {
            if (user.getExpectedSalary() == null) {
                score += 5; // Không nhập lương thì vẫn không loại bỏ hoàn toàn
            } else if (user.getExpectedSalary() <= maxExpectedSalary) {
                score += 15;
            }
        }

        if (notBlank(workType) && equalsIgnoreCase(user.getWorkType(), workType)) {
            score += 10;
        }

        if (notBlank(educationLevel) && containsIgnoreCase(user.getEducationLevel(), educationLevel)) {
            score += 5;
        }

        if (notBlank(englishLevel) && containsIgnoreCase(user.getEnglishLevel(), englishLevel)) {
            score += 5;
        }

        if (notBlank(user.getDesiredPosition()) && containsIgnoreCase(job.getTitle(), user.getDesiredPosition())) {
            score += 10;
        }

        if (notBlank(user.getDesiredCategory()) && containsIgnoreCase(job.getCategory(), user.getDesiredCategory())) {
            score += 10;
        }

        return score;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (!notBlank(source) || !notBlank(keyword)) {
            return false;
        }

        return source.toLowerCase().contains(keyword.toLowerCase());
    }

    private boolean equalsIgnoreCase(String source, String target) {
        if (!notBlank(source) || !notBlank(target)) {
            return false;
        }

        return source.equalsIgnoreCase(target);
    }
}