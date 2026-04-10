package com.jobconnect.entity;

public enum UserStatus {
    UNVERIFIED, // Mới đăng ký, chưa xác thực Email
    ACTIVE,     // Đã xác thực, hoạt động bình thường
    BANNED,     // Bị admin hoặc hệ thống khóa tài khoản (do vi phạm hoặc lý do khác)
    DELETED     // Đã xóa tài khoản (Xóa mềm - không hiện trên app nhưng vẫn còn trong DB)
}