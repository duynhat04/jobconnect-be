package com.jobconnect.service;

import com.jobconnect.entity.JobPackage;
import com.jobconnect.exception.ResourceNotFoundException;
import com.jobconnect.repository.JobPackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobPackageService {

    @Autowired
    private JobPackageRepository packageRepository;

    public List<JobPackage> getAllPackages() {
        return packageRepository.findAll();
    }

    public List<JobPackage> getActivePackages() {
        return packageRepository.findByIsActiveTrue();
    }

    public JobPackage createPackage(JobPackage jobPackage) {
        return packageRepository.save(jobPackage);
    }

    public JobPackage updatePackage(Long id, JobPackage packageDetails) {
        JobPackage existingPackage = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói dịch vụ với ID: " + id));

        existingPackage.setName(packageDetails.getName());
        existingPackage.setPrice(packageDetails.getPrice());
        existingPackage.setPostLimit(packageDetails.getPostLimit());
        existingPackage.setDurationDays(packageDetails.getDurationDays());
        existingPackage.setIsPopular(packageDetails.getIsPopular());
        existingPackage.setIsActive(packageDetails.getIsActive());

        return packageRepository.save(existingPackage);
    }

    public void togglePackageStatus(Long id) {
        JobPackage existingPackage = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói dịch vụ với ID: " + id));
        
        existingPackage.setIsActive(!existingPackage.getIsActive());
        packageRepository.save(existingPackage);
    }
}