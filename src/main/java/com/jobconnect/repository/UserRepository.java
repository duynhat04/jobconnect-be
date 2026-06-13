package com.jobconnect.repository;

import com.jobconnect.entity.User;
import com.jobconnect.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByIdIn(List<Long> ids);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<User> searchByEmailOrName(
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
            AND (:status IS NULL OR u.status = :status)
            AND (
                :search IS NULL
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            """)
    Page<User> findCandidatesWithFilter(
            @Param("role") String role,
            @Param("status") UserStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    long countByRole(String role);
}