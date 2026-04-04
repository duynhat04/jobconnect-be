package com.jobconnect.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.JobApplication;
import com.jobconnect.entity.User;
import com.jobconnect.entity.UserCV;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserCVRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
            throw new RuntimeException("Bạn đã ứng tuyển vào công việc này rồi! Vui lòng chờ phản hồi từ nhà tuyển dụng.");
        }
    }

    // LUỒNG 1: TẢI CV MỚI LÊN (Upload Cloudinary)
    public JobApplication applyWithNewCV(String userEmail, Long jobId, MultipartFile cvFile, String coverLetter) throws Exception {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc này!"));

        // KIỂM TRA TRÙNG LẶP Ở ĐÂY
        checkDuplicateApplication(user, job);

        // Đẩy lên Cloudinary lấy link
        Map<String, Object> uploadOptions = new java.util.HashMap<>();
        uploadOptions.put("resource_type", "auto");
        Map uploadResult = cloudinary.uploader().upload(cvFile.getBytes(), uploadOptions);
        String cvUrl = uploadResult.get("secure_url").toString();

        // Lưu thông tin ứng tuyển
        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setJob(job);
        application.setCvUrl(cvUrl);
        application.setCoverLetter(coverLetter);

        JobApplication savedApplication = applicationRepository.save(application);

        // BẮN THÔNG BÁO CHO NHÀ TUYỂN DỤNG
        try {
            notificationService.createNotification(
                    savedApplication.getJob().getCompany().getUser().getId(),
                    "Có ứng tuyển mới!",
                    "Ứng viên " + savedApplication.getUser().getFullName() + " vừa nộp CV vào vị trí " + savedApplication.getJob().getTitle(),
                    "NEW_APPLICATION",
                    "/employer/manage-applications"
            );
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi bắn thông báo: " + e.getMessage());
        }

        return savedApplication;
    }

    // LUỒNG 2: DÙNG CV ĐÃ LƯU TRONG HỆ THỐNG (Không cần gọi Cloudinary)
    public JobApplication applyWithExistingCV(String userEmail, Long jobId, Long userCvId, String coverLetter) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc này!"));

        // KIỂM TRA TRÙNG LẶP Ở ĐÂY
        checkDuplicateApplication(user, job);

        // Tìm cái CV mà user đã chọn
        UserCV userCV = userCVRepository.findById(userCvId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy CV đã lưu!"));

        // Bảo mật: Kiểm tra xem cái CV đó có đúng là của thằng đang nộp không
        if (!userCV.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Lỗi bảo mật: Bạn không có quyền sử dụng CV này!");
        }

        // Tạo hồ sơ ứng tuyển
        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setJob(job);
        application.setCvUrl(userCV.getFileUrl());
        application.setCoverLetter(coverLetter);

        JobApplication savedApplication = applicationRepository.save(application);

        // BẮN THÔNG BÁO CHO NHÀ TUYỂN DỤNG
        try {
            notificationService.createNotification(
                    savedApplication.getJob().getCompany().getUser().getId(),
                    "Có ứng tuyển mới!",
                    "Ứng viên " + savedApplication.getUser().getFullName() + " vừa sử dụng CV đã lưu để nộp vào vị trí " + savedApplication.getJob().getTitle(),
                    "NEW_APPLICATION",
                    "/employer/manage-applications"
            );
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi bắn thông báo: " + e.getMessage());
        }

        return savedApplication;
    }

    // LUỒNG 3: NỘP CV CHUNG
    public JobApplication applyJob(String userEmail, Long jobId, MultipartFile cvFile, String coverLetter) throws Exception {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc này!"));

        Map<String, Object> uploadOptions = new java.util.HashMap<>();
        uploadOptions.put("resource_type", "auto");

        Map uploadResult = cloudinary.uploader().upload(cvFile.getBytes(), uploadOptions);
        String cvUrl = uploadResult.get("secure_url").toString();

        JobApplication application = new JobApplication();
        application.setUser(user);
        application.setJob(job);
        application.setCvUrl(cvUrl);
        application.setCoverLetter(coverLetter);

        JobApplication savedApplication = applicationRepository.save(application);

        // BẮN THÔNG BÁO CHO NHÀ TUYỂN DỤNG
        try {
            notificationService.createNotification(
                    savedApplication.getJob().getCompany().getUser().getId(),
                    "Có ứng tuyển mới!",
                    "Ứng viên " + savedApplication.getUser().getFullName() + " vừa nộp CV vào vị trí " + savedApplication.getJob().getTitle(),
                    "NEW_APPLICATION",
                    "/employer/manage-applications"
            );
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi bắn thông báo: " + e.getMessage());
        }

        return savedApplication;
    }

    // 1. Ứng viên xem lịch sử nộp CV của chính mình
    public List<JobApplication> getMyApplications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        return applicationRepository.findByUserId(user.getId());
    }

    // 2. Nhà tuyển dụng xem danh sách ứng viên nộp vào 1 Job
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
    public JobApplication updateApplicationStatus(Long applicationId, String newStatus, String employerEmail) {

        User employer = userRepository.findByEmail(employerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ ứng tuyển!"));

        // CHỐNG DUYỆT TRỘM
        if (!application.getJob().getCompany().getUser().getId().equals(employer.getId())) {
            throw new RuntimeException("LỖI BẢO MẬT: Bạn không có quyền duyệt CV của công ty khác!");
        }

        // Cập nhật trạng thái mới và Lưu vào Database
        application.setStatus(newStatus.toUpperCase());
        JobApplication savedApp = applicationRepository.save(application);

        // BẮN THÔNG BÁO CHO ỨNG VIÊN TRƯỚC
        try {
            String statusVN = "ACCEPTED".equalsIgnoreCase(newStatus) ? "được CHẤP NHẬN" : "bị TỪ CHỐI";
            notificationService.createNotification(
                    savedApp.getUser().getId(), // Bắn về ID của Ứng viên
                    "Cập nhật trạng thái ứng tuyển",
                    "Hồ sơ của bạn tại vị trí " + savedApp.getJob().getTitle() + " đã " + statusVN,
                    "APPLICATION_STATUS",
                    "/candidate/my-applications"
            );
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi bắn thông báo: " + e.getMessage());
        }

        // BẮN EMAIL TỰ ĐỘNG CHO ỨNG VIÊN
        try {
            String candidateEmail = application.getUser().getEmail();
            String jobTitle = application.getJob().getTitle();
            String companyName = application.getJob().getCompany().getName();

            if ("ACCEPTED".equalsIgnoreCase(newStatus)) {
                String subject = "🎉 Chúc mừng! Hồ sơ của bạn đã được duyệt";
                String body = "Chào bạn,\n\n" +
                        "Công ty " + companyName + " đã xem xét hồ sơ của bạn cho vị trí " + jobTitle + ".\n" +
                        "Chúng tôi rất ấn tượng và muốn mời bạn tham gia vòng phỏng vấn tiếp theo.\n\n" +
                        "Bộ phận nhân sự sẽ sớm liên hệ với bạn để chốt lịch hẹn.\n\n" +
                        "Trân trọng,\n" + companyName;

                emailService.sendEmail(candidateEmail, subject, body);

            } else if ("REJECTED".equalsIgnoreCase(newStatus)) {
                String subject = "Thông báo kết quả ứng tuyển vị trí " + jobTitle;
                String body = "Chào bạn,\n\n" +
                        "Cảm ơn bạn đã quan tâm và ứng tuyển vào vị trí " + jobTitle + " tại " + companyName + ".\n" +
                        "Rất tiếc, hồ sơ của bạn chưa phù hợp với tiêu chí của chúng tôi ở thời điểm hiện tại.\n\n" +
                        "Chúc bạn may mắn và thành công trên con đường sự nghiệp!\n\n" +
                        "Trân trọng,\n" + companyName;

                emailService.sendEmail(candidateEmail, subject, body);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khi gửi email (nhưng CV vẫn được duyệt): " + e.getMessage());
        }

        return savedApp;
    }

    // 4. Nhà tuyển dụng lấy TẤT CẢ CV đổ về công ty mình
    public List<JobApplication> getAllApplicationsForMyCompany(String employerEmail) {
        return applicationRepository.findByJob_Company_User_EmailOrderByIdDesc(employerEmail);
    }
}