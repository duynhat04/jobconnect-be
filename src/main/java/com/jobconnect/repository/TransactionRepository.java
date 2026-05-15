package com.jobconnect.repository;

import com.jobconnect.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Tìm giao dịch dựa trên mã thanh toán gửi cho VNPay
    Optional<Transaction> findByTxnRef(String txnRef);

    // Lấy tất cả giao dịch từ 1 mốc thời gian trở về hiện tại (Dùng để tính tổng tiền và vẽ biểu đồ)
    List<Transaction> findByCreatedAtAfter(LocalDateTime startDate);
    
    // Lấy danh sách giao dịch mới nhất (Dùng cho bảng Recent Transactions)
    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}