package com.jobconnect.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequestDTO {
    private Long packageId;
    private BigDecimal amount;
}