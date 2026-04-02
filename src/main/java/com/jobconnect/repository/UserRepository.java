package com.jobconnect.repository;

import com.jobconnect.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Tìm kiếm User theo Email (Trả về Optional để dễ xử lý lỗi nếu không tìm thấy)
    Optional<User> findByEmail(String email);

    // Tìm kiếm User theo email (dành cho Admin dùng thanh search)
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
}