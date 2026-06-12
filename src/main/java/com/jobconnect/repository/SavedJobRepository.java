package com.jobconnect.repository;

import com.jobconnect.entity.SavedJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    Optional<SavedJob> findByUserIdAndJobId(Long userId, Long jobId);

    @EntityGraph(attributePaths = {
            "job",
            "job.company"
    })
    List<SavedJob> findByUserIdOrderBySavedAtDesc(Long userId);

    long countByUserId(Long userId);

    void deleteByUserIdAndJobId(Long userId, Long jobId);
}