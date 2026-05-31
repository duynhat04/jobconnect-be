package com.jobconnect.service;

import com.jobconnect.dto.JobRequest;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.User;
import com.jobconnect.repository.CompanyRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        category = category.trim().replaceAll("\\s+", " ");

        List<String> upperCaseWords = List.of(
                "IT", "AI", "HR", "UI", "UX",
                "QA", "SEO", "DEVOPS", "PM", "BA");

        String[] words = category.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            String upperWord = word.toUpperCase();
            // Nếu là từ viết tắt => giữ nguyên in hoa
            if (upperCaseWords.contains(upperWord)) {
                result.append(upperWord);
            } else {
                // capitalize bình thường
                result.append(
                        Character.toUpperCase(word.charAt(0))
                                + word.substring(1).toLowerCase());
            }
            result.append(" ");
        }
        return result.toString().trim();
    }

    @Transactional
    public Job createJob(JobRequest jobRequest, String employerEmail) {

        Company company = companyRepository.findByUser_Email(employerEmail)
                .orElseThrow(
                        () -> new RuntimeException("Không tìm thấy công ty của bạn. Vui lòng cập nhật hồ sơ công ty!"));

        // Chốt chặn 1: Trạng thái duyệt công ty
        if (company.getStatus() == null || !company.getStatus().equals("APPROVED")) {
            throw new RuntimeException("Công ty của bạn chưa được xác thực. Vui lòng chờ Admin duyệt công ty trước!");
        }

        // Chốt chặn 2: KIỂM TRA LƯỢT ĐĂNG TIN (BỔ SUNG)
        Integer remaining = company.getRemainingPosts();
        if (remaining == null || remaining <= 0) {
            throw new RuntimeException("Tài khoản của bạn đã hết lượt đăng tin. Vui lòng nâng cấp gói cước!");
        }

        // --- THỰC HIỆN TRỪ TIN ---
        company.setRemainingPosts(remaining - 1);
        companyRepository.save(company);

        Job job = new Job();
        job.setTitle(jobRequest.getTitle());
        job.setDescription(jobRequest.getDescription());
        job.setLocation(jobRequest.getLocation());
        job.setSalary(jobRequest.getSalary());
        job.setCategory(normalizeCategory(jobRequest.getCategory()));
        job.setRequirements(jobRequest.getRequirements());

        job.setCompany(company);
        job.setStatus("PENDING");

        return jobRepository.save(job);
    }

    // 2. CẬP NHẬT JOB
    @Transactional // Nên có để an toàn dữ liệu
    public Job updateJob(Long id, JobRequest jobRequest, String email) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + id));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        Company company = currentUser.getCompany();

        if (company == null) {
            throw new RuntimeException("Tài khoản chưa được liên kết với công ty nào!");
        }

        if (!existingJob.getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa công việc của công ty khác!");
        }

        existingJob.setTitle(jobRequest.getTitle());
        existingJob.setLocation(jobRequest.getLocation());
        existingJob.setSalary(jobRequest.getSalary());
        existingJob.setDescription(jobRequest.getDescription());
        existingJob.setRequirements(jobRequest.getRequirements());
        existingJob.setCategory(normalizeCategory(jobRequest.getCategory()));

        return jobRepository.save(existingJob);
    }

    // --- XÓA JOB ---
    @Transactional
    public void deleteJob(Long id, String email) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + id));

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        Company company = currentUser.getCompany();

        if (company == null) {
            throw new RuntimeException("Tài khoản chưa được liên kết với công ty nào!");
        }

        if (!existingJob.getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa công việc của công ty khác!");
        }

        jobRepository.delete(existingJob);
    }

    // 3. ADMIN DUYỆT BÀI
    public Job approveJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng!"));
        job.setStatus("APPROVED");
        job.setRejectionReason(null);
        return jobRepository.save(job);
    }

    // 4. ADMIN TỪ CHỐI BÀI KÈM LÝ DO
    public Job rejectJob(Long jobId, String reason) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng!"));
        job.setStatus("REJECTED");
        job.setRejectionReason(reason);
        return jobRepository.save(job);
    }

    // 5. Lấy TẤT CẢ Job (Dành cho Admin quản lý)
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    // 6. Lấy Job ĐÃ DUYỆT (Dành cho Ứng viên xem trên trang chủ)
    public List<Job> getApprovedJobs() {
        return jobRepository.findByStatus("APPROVED");
    }

    // 7. Tìm kiếm, Lọc và Phân trang
    public Page<Job> searchJobs(String keyword, String location, String category, Long minSalary, int page, int size) {
        // Chuẩn thực tế: Luôn sort theo ngày tạo giảm dần (Mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return jobRepository.searchAndFilterJobs(keyword, location, category, minSalary, pageable);
    }

    // 8. Lấy chi tiết Job bằng ID
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + id));
    }

    // 9. Lấy danh sách Job của một Nhà tuyển dụng cụ thể
    public List<Job> getMyJobs(String email) {
        return jobRepository.findByCompany_User_Email(email);
    }

    // 10. Lấy danh sách category duy nhất
    public List<String> getAllCategories() {
        return jobRepository.findDistinctCategories();
    }

    public List<Job> getRelatedJobs(Long jobId) {

        Job currentJob = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        Pageable pageable = PageRequest.of(0, 4);

        return jobRepository.findRelatedJobs(
                currentJob.getId(),
                currentJob.getCategory(),
                currentJob.getCompany().getId(),
                pageable);
    }

    @Transactional(readOnly = true)
    public Page<Job> getPublicJobsByCompany(Long companyId, int page, int size) {
        if (page < 0) {
            page = 0;
        }

        if (size <= 0) {
            size = 6;
        }

        if (size > 30) {
            size = 30;
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return jobRepository.findByCompany_IdAndStatusOrderByCreatedAtDesc(
                companyId,
                "APPROVED",
                pageable);
    }
}