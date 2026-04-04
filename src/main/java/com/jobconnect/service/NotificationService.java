package com.jobconnect.service;

import com.jobconnect.entity.Notification;
import com.jobconnect.entity.User;
import com.jobconnect.repository.NotificationRepository;
import com.jobconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Notification> getMyNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Notification markAsRead(Long id) {
        Notification notif = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        notif.setRead(true);
        return notificationRepository.save(notif);
    }

    // THÊM HÀM TẠO THÔNG BÁO MỚI
    public Notification createNotification(Long userId, String title, String message, String type, String targetUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Notification notif = new Notification();
        notif.setUser(user);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setType(type);
        notif.setTargetUrl(targetUrl);

        return notificationRepository.save(notif);
    }

    @org.springframework.transaction.annotation.Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Lấy hết thông báo CHƯA ĐỌC của user này ra
        List<Notification> unreadNotifs = notificationRepository.findByUserIdAndIsReadFalse(user.getId());

        // Set thành đã đọc hết
        for (Notification notif : unreadNotifs) {
            notif.setRead(true);
        }

        // Lưu lại 1 loạt vào DB
        notificationRepository.saveAll(unreadNotifs);
    }
}