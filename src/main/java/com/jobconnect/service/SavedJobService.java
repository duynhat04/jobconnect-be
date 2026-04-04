package com.jobconnect.service;

import com.jobconnect.entity.Job;
import com.jobconnect.entity.SavedJob;
import com.jobconnect.entity.User;
import com.jobconnect.repository.JobRepository;
import com.jobconnect.repository.SavedJobRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SavedJobService {

    @Autowired
    private SavedJobRepository savedJobRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JobRepository jobRepository;

    // Xem danh sách job đã lưu
    public List<SavedJob> getMySavedJobs(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        return savedJobRepository.findByUserIdOrderBySavedAtDesc(user.getId());
    }

    // Bấm nút Lưu Job (Nếu lưu rồi thì báo lỗi, chưa thì lưu)
    public SavedJob saveJob(String email, Long jobId) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));

        if (savedJobRepository.existsByUserIdAndJobId(user.getId(), jobId)) {
            throw new RuntimeException("Bạn đã lưu công việc này rồi!");
        }

        SavedJob savedJob = new SavedJob();
        savedJob.setUser(user);
        savedJob.setJob(job);
        return savedJobRepository.save(savedJob);
    }

    // Bấm nút Bỏ lưu Job
    public void unsaveJob(String email, Long jobId) {
        User user = userRepository.findByEmail(email).orElseThrow();
        SavedJob savedJob = savedJobRepository.findByUserIdAndJobId(user.getId(), jobId)
                .orElseThrow(() -> new RuntimeException("Chưa lưu công việc này!"));

        savedJobRepository.delete(savedJob);
    }
}