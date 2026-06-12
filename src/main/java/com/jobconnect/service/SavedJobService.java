package com.jobconnect.service;

import com.jobconnect.dto.SavedJobResponse;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.Job;
import com.jobconnect.entity.SavedJob;
import com.jobconnect.entity.User;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.SavedJobRepository;
import com.jobconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    @Transactional
    public SavedJobResponse saveJob(String userEmail, Long jobId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        Job job = jobRepository.findByIdWithCompany(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc!"));

        if (!"APPROVED".equalsIgnoreCase(job.getStatus())) {
            throw new RuntimeException("Chỉ có thể lưu tin tuyển dụng đang hiển thị!");
        }

        if (job.getExpiredAt() != null && job.getExpiredAt().isBefore(LocalDate.now())) {
            throw new RuntimeException("Tin tuyển dụng đã hết hạn, không thể lưu!");
        }

        if (savedJobRepository.existsByUserIdAndJobId(user.getId(), jobId)) {
            throw new RuntimeException("Bạn đã lưu công việc này rồi!");
        }

        SavedJob savedJob = SavedJob.builder()
                .user(user)
                .job(job)
                .build();

        SavedJob result = savedJobRepository.save(savedJob);

        return toResponse(result);
    }

    @Transactional
    public void unsaveJob(String userEmail, Long jobId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        SavedJob savedJob = savedJobRepository.findByUserIdAndJobId(user.getId(), jobId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa lưu công việc này!"));

        savedJobRepository.delete(savedJob);
    }

    @Transactional(readOnly = true)
    public List<SavedJobResponse> getMySavedJobs(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        return savedJobRepository.findByUserIdOrderBySavedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isJobSaved(String userEmail, Long jobId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        return savedJobRepository.existsByUserIdAndJobId(user.getId(), jobId);
    }

    private SavedJobResponse toResponse(SavedJob savedJob) {
        Job job = savedJob.getJob();
        Company company = job != null ? job.getCompany() : null;

        return SavedJobResponse.builder()
                .id(savedJob.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : null)
                .jobLocation(job != null ? job.getLocation() : null)
                .jobSalary(job != null ? job.getSalary() : null)
                .jobCategory(job != null ? job.getCategory() : null)
                .employmentType(
                        job != null && job.getEmploymentType() != null
                                ? job.getEmploymentType().name()
                                : null
                )
                .jobStatus(job != null ? job.getStatus() : null)
                .companyId(company != null ? company.getId() : null)
                .companyName(company != null ? company.getName() : null)
                .companyLogo(company != null ? company.getLogo() : null)
                .savedAt(savedJob.getSavedAt())
                .build();
    }
}