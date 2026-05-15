package com.jobconnect.repository;

import com.jobconnect.entity.JobPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPackageRepository extends JpaRepository<JobPackage, Long> {
    List<JobPackage> findByIsActiveTrue();
}