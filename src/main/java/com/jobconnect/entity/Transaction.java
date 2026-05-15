package com.jobconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(nullable = false)
    private BigDecimal amount;

    // Trạng thái: PENDING, SUCCESS, FAILED
    @Column(nullable = false, length = 20)
    private String status;

    // Mã giao dịch gửi sang VNPay (Phải duy nhất)
    @Column(name = "txn_ref", nullable = false, unique = true, length = 50)
    private String txnRef;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod; // VD: "VNPAY"

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}