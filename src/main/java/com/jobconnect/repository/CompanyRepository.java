package com.jobconnect.repository;

import com.jobconnect.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByTaxCode(String taxCode);

    boolean existsByUserId(Long userId);

    List<Company> findTop5ByOrderByIdDesc();

    Page<Company> findByStatus(String status, Pageable pageable);

    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Company> findByStatusOrderByIdDesc(String status, Pageable pageable);

    Page<Company> findByNameContainingIgnoreCaseAndStatusOrderByIdDesc(
            String name,
            String status,
            Pageable pageable);

  

    java.util.Optional<Company> findByUser_Email(String email);
}