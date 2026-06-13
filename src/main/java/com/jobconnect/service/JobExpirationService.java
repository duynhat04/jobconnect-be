package com.jobconnect.service;

import com.jobconnect.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExpirationService {

    private final JobRepository jobRepository;

    /**
     * Chạy 1 lần khi BE khởi động.
     * Repository method đã có @Transactional nên update query chạy an toàn.
     */
    @PostConstruct
    public void updateExpiredJobsOnStartup() {
        updateExpiredJobs("khởi động hệ thống");
    }

    /**
     * Tự động chạy mỗi ngày lúc 00:05 theo giờ Việt Nam.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void updateExpiredJobsDaily() {
        updateExpiredJobs("lịch tự động hằng ngày");
    }

    private void updateExpiredJobs(String source) {
        try {
            int updated = jobRepository.markExpiredApprovedJobs();

            if (updated > 0) {
                log.info("Đã cập nhật {} tin tuyển dụng hết hạn từ {}.", updated, source);
            }
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật tin tuyển dụng hết hạn từ {}: {}", source, e.getMessage(), e);
        }
    }
}