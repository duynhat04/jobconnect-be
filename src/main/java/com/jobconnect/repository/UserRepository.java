package com.jobconnect.repository;

import com.jobconnect.dto.UserProfileDto;
import com.jobconnect.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Tìm kiếm User theo Email (Trả về Optional để dễ xử lý lỗi nếu không tìm thấy)
    Optional<User> findByEmail(String email);

    List<User> findByIdIn(List<Long> ids);

    //Tìm kiếm đa năng theo cả Email lẫn Họ Tên
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchByEmailOrName(@Param("search") String search, Pageable pageable);

    // [THÊM MỚI] - Lọc theo Role, Status (Enum) và Search
    @Query("SELECT u FROM User u WHERE u.role = :role " +
           "AND (:status IS NULL OR u.status = :status) " +
           "AND (:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findCandidatesWithFilter(
            @Param("role") String role, 
            @Param("status") com.jobconnect.entity.UserStatus status, 
            @Param("search") String search, 
            Pageable pageable);
       
}