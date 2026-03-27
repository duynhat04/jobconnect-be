package com.jobconnect.repository;

import com.jobconnect.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    // Để sau này Nhà tuyển dụng xem danh sách CV nộp vào Job của họ
    List<JobApplication> findByJobId(Long jobId);

    // Để Ứng viên xem lịch sử những Job mình đã nộp
    List<JobApplication> findByUserId(Long userId);
}