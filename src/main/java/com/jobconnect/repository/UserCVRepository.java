package com.jobconnect.repository;

import com.jobconnect.entity.UserCV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCVRepository extends JpaRepository<UserCV, Long> {
    List<UserCV> findByUserId(Long userId);
}