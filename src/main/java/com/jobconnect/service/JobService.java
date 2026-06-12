package com.jobconnect.service;

import com.jobconnect.dto.JobApplicationCountProjection;
import com.jobconnect.dto.JobRequest;
import com.jobconnect.dto.JobResponse;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.EmploymentType;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.User;
import com.jobconnect.repository.CompanyRepository;
import com.jobconnect.repository.JobApplicationRepository;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    // =========================
    // CREATE JOB
    // =========================

    @Transactional
    public JobResponse createJob(JobRequest jobRequest, String employerEmail) {
        Company company = companyRepository.findByUser_Email(employerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty của bạn. Vui lòng cập nhật hồ sơ công ty!"));

        if (company.getStatus() == null || !"APPROVED".equalsIgnoreCase(company.getStatus())) {
            throw new RuntimeException("Công ty của bạn chưa được xác thực. Vui lòng chờ Admin duyệt công ty trước!");
        }

        Integer remaining = company.getRemainingPosts();
        if (remaining == null || remaining <= 0) {
            throw new RuntimeException("Tài khoản của bạn đã hết lượt đăng tin. Vui lòng nâng cấp gói cước!");
        }

        validateExpiredAt(jobRequest.getExpiredAt());

        company.setRemainingPosts(remaining - 1);
        companyRepository.save(company);

        Job job = new Job();
        job.setTitle(normalize(jobRequest.getTitle()));
        job.setDescription(normalize(jobRequest.getDescription()));
        job.setLocation(normalize(jobRequest.getLocation()));
        job.setSalary(jobRequest.getSalary());
        job.setCategory(normalizeCategory(jobRequest.getCategory()));
        job.setRequirements(normalize(jobRequest.getRequirements()));
        job.setEmploymentType(jobRequest.getEmploymentType());
        job.setExpiredAt(jobRequest.getExpiredAt());
        job.setCompany(company);
        job.setStatus("PENDING");

        Job savedJob = jobRepository.save(job);

        return toJobResponse(savedJob);
    }

    // =========================
    // UPDATE JOB
    // =========================

    @Transactional
    public JobResponse updateJob(Long id, JobRequest jobRequest, String email) {
        Job existingJob = jobRepository.findByIdWithCompany(id)
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

        validateExpiredAt(jobRequest.getExpiredAt());

        existingJob.setTitle(normalize(jobRequest.getTitle()));
        existingJob.setLocation(normalize(jobRequest.getLocation()));
        existingJob.setSalary(jobRequest.getSalary());
        existingJob.setDescription(normalize(jobRequest.getDescription()));
        existingJob.setRequirements(normalize(jobRequest.getRequirements()));
        existingJob.setCategory(normalizeCategory(jobRequest.getCategory()));
        existingJob.setEmploymentType(jobRequest.getEmploymentType());
        existingJob.setExpiredAt(jobRequest.getExpiredAt());

        Job savedJob = jobRepository.save(existingJob);

        return toJobResponse(savedJob);
    }

    // =========================
    // DELETE JOB
    // =========================

    @Transactional
    public void deleteJob(Long id, String email) {
        Job existingJob = jobRepository.findByIdWithCompany(id)
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

    // =========================
    // CLOSE JOB
    // =========================

    @Transactional
    public JobResponse closeJob(Long id, String email) {
        Job job = jobRepository.findByIdWithCompany(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + id));

        Company company = companyRepository.findByUser_Email(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty của bạn!"));

        if (!job.getCompany().getId().equals(company.getId())) {
            throw new RuntimeException("Bạn không có quyền đóng tin tuyển dụng của công ty khác!");
        }

        if ("CLOSED".equalsIgnoreCase(job.getStatus())) {
            throw new RuntimeException("Tin tuyển dụng này đã được đóng trước đó!");
        }

        job.setStatus("CLOSED");

        Job savedJob = jobRepository.save(job);

        return toJobResponse(savedJob);
    }

    // =========================
    // ADMIN APPROVE / REJECT
    // =========================

    @Transactional
    public JobResponse approveJob(Long jobId) {
        Job job = jobRepository.findByIdWithCompany(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng!"));

        job.setStatus("APPROVED");
        job.setRejectionReason(null);

        Job savedJob = jobRepository.save(job);

        return toJobResponse(savedJob);
    }

    @Transactional
    public JobResponse rejectJob(Long jobId, String reason) {
        Job job = jobRepository.findByIdWithCompany(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng!"));

        job.setStatus("REJECTED");
        job.setRejectionReason(normalize(reason));

        Job savedJob = jobRepository.save(job);

        return toJobResponse(savedJob);
    }

    // =========================
    // ADMIN GET ALL ENTITY
    // =========================

    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    // =========================
    // PUBLIC JOBS
    // =========================

    @Transactional(readOnly = true)
    public List<JobResponse> getApprovedJobs() {
        List<Job> jobs = jobRepository.findApprovedJobsWithCompany(
                "APPROVED",
                LocalDate.now()
        );

        return toJobResponseList(jobs);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobs(
            String keyword,
            String location,
            String category,
            Long minSalary,
            EmploymentType employmentType,
            int page,
            int size
    ) {
        page = normalizePage(page);
        size = normalizeSize(size, 50);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Job> jobPage = jobRepository.searchAndFilterJobs(
                cleanSearchValue(keyword),
                cleanSearchValue(location),
                cleanSearchValue(category),
                minSalary,
                employmentType,
                pageable
        );

        List<JobResponse> responses = toJobResponseList(jobPage.getContent());

        return new PageImpl<>(
                responses,
                pageable,
                jobPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findByIdWithCompany(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + id));

        return toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> getRelatedJobs(Long jobId) {
        Job currentJob = jobRepository.findRelatedSourceJobWithCompany(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        Pageable pageable = PageRequest.of(0, 4);

        List<Job> jobs = jobRepository.findRelatedJobs(
                currentJob.getId(),
                currentJob.getCategory(),
                currentJob.getCompany().getId(),
                pageable
        );

        return toJobResponseList(jobs);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getPublicJobsByCompany(Long companyId, int page, int size) {
        page = normalizePage(page);
        size = normalizeSize(size, 30);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Job> jobPage = jobRepository.findByCompany_IdAndStatusAndExpiredAtGreaterThanEqualOrderByCreatedAtDesc(
                companyId,
                "APPROVED",
                LocalDate.now(),
                pageable
        );

        List<JobResponse> responses = toJobResponseList(jobPage.getContent());

        return new PageImpl<>(
                responses,
                pageable,
                jobPage.getTotalElements()
        );
    }

    // =========================
    // EMPLOYER JOBS
    // =========================

    @Transactional(readOnly = true)
    public List<JobResponse> getMyJobs(String email) {
        List<Job> jobs = jobRepository.findMyJobsWithCompany(email);

        return toJobResponseList(jobs);
    }

    // =========================
    // CATEGORY
    // =========================

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return jobRepository.findDistinctCategories();
    }

    // =========================
    // MAPPER LIST - OPTIMIZED COUNT
    // =========================

    private List<JobResponse> toJobResponseList(List<Job> jobs) {
        Map<Long, JobApplicationCountProjection> countMap = getApplicationCountMap(jobs);

        return jobs.stream()
                .map(job -> toJobResponse(job, countMap.get(job.getId())))
                .collect(Collectors.toList());
    }

    private Map<Long, JobApplicationCountProjection> getApplicationCountMap(List<Job> jobs) {
        List<Long> jobIds = jobs.stream()
                .map(Job::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (jobIds.isEmpty()) {
            return Map.of();
        }

        return jobApplicationRepository.countApplicationsByJobIds(jobIds)
                .stream()
                .collect(Collectors.toMap(
                        JobApplicationCountProjection::getJobId,
                        count -> count
                ));
    }

    // =========================
    // MAPPER SINGLE
    // =========================

    private JobResponse toJobResponse(Job job) {
        List<JobApplicationCountProjection> counts =
                jobApplicationRepository.countApplicationsByJobIds(List.of(job.getId()));

        JobApplicationCountProjection count = counts.isEmpty() ? null : counts.get(0);

        return toJobResponse(job, count);
    }

    private JobResponse toJobResponse(Job job, JobApplicationCountProjection count) {
        JobResponse response = new JobResponse();

        response.setId(job.getId());
        response.setTitle(job.getTitle());
        response.setDescription(job.getDescription());
        response.setLocation(job.getLocation());
        response.setSalary(job.getSalary());
        response.setRequirements(job.getRequirements());
        response.setCategory(job.getCategory());
        response.setEmploymentType(job.getEmploymentType());
        response.setCreatedAt(job.getCreatedAt());
        response.setExpiredAt(job.getExpiredAt());
        response.setStatus(job.getStatus());
        response.setRejectionReason(job.getRejectionReason());

        Company company = job.getCompany();

        if (company != null) {
            response.setCompanyId(company.getId());
            response.setCompanyName(company.getName());
            response.setCompanyLogo(company.getLogo());
            response.setCompanyAddress(company.getAddress());
        }

        if (count != null) {
            response.setApplicationCount(defaultLong(count.getApplicationCount()));
            response.setPendingApplicationCount(defaultLong(count.getPendingApplicationCount()));
            response.setReviewedApplicationCount(defaultLong(count.getReviewedApplicationCount()));
            response.setAcceptedApplicationCount(defaultLong(count.getAcceptedApplicationCount()));
            response.setRejectedApplicationCount(defaultLong(count.getRejectedApplicationCount()));
        } else {
            response.setApplicationCount(0);
            response.setPendingApplicationCount(0);
            response.setReviewedApplicationCount(0);
            response.setAcceptedApplicationCount(0);
            response.setRejectedApplicationCount(0);
        }

        response.setExpired(isExpired(job));
        response.setClosed("CLOSED".equalsIgnoreCase(job.getStatus()));
        response.setDaysRemaining(calculateDaysRemaining(job));

        return response;
    }

    // =========================
    // HELPER
    // =========================

    private void validateExpiredAt(LocalDate expiredAt) {
        if (expiredAt == null) {
            throw new RuntimeException("Vui lòng chọn ngày hết hạn bài đăng!");
        }

        if (!expiredAt.isAfter(LocalDate.now())) {
            throw new RuntimeException("Ngày hết hạn phải lớn hơn ngày hiện tại!");
        }
    }

    private boolean isExpired(Job job) {
        return job.getExpiredAt() != null && job.getExpiredAt().isBefore(LocalDate.now());
    }

    private long calculateDaysRemaining(Job job) {
        if (job.getExpiredAt() == null) {
            return 0;
        }

        long days = ChronoUnit.DAYS.between(LocalDate.now(), job.getExpiredAt());

        return Math.max(days, 0);
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size, int maxSize) {
        if (size <= 0) {
            return 10;
        }

        return Math.min(size, maxSize);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private String cleanSearchValue(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }

        category = category.trim().replaceAll("\\s+", " ");

        List<String> upperCaseWords = List.of(
                "IT", "AI", "HR", "UI", "UX",
                "QA", "SEO", "DEVOPS", "PM", "BA"
        );

        String[] words = category.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            String upperWord = word.toUpperCase();

            if (upperCaseWords.contains(upperWord)) {
                result.append(upperWord);
            } else {
                result.append(
                        Character.toUpperCase(word.charAt(0))
                                + word.substring(1).toLowerCase()
                );
            }

            result.append(" ");
        }

        return result.toString().trim();
    }
}