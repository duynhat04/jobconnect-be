package com.jobconnect.repository;

import com.jobconnect.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
    // Không cần viết thêm hàm gì, JpaRepository đã lo đủ findAll và save
}