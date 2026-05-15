package com.jobconnect.repository;

import com.jobconnect.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // LẤY TOP 30 THÔNG BÁO MỚI NHẤT 
    List<Notification> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);

    // Đếm số lượng chưa đọc
    long countByUserIdAndIsReadFalse(Long userId);

    //UPDATE THẲNG XUỐNG DB 
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(Long userId);
}