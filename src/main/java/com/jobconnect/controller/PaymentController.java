package com.jobconnect.controller;

import com.jobconnect.dto.PaymentRequestDTO;
import com.jobconnect.dto.PaymentResponseDTO;
import com.jobconnect.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // API 1: Dành cho Frontend gọi khi bấm nút "Mua gói"
    @PostMapping("/create-url")
    public ResponseEntity<PaymentResponseDTO> createPaymentUrl(@RequestBody PaymentRequestDTO requestDTO, HttpServletRequest request) {
        
        // TODO: Lấy ID người dùng thực tế từ Security Context (Token)
        // Long employerId = securityUtils.getCurrentUserId();
        Long mockEmployerId = 1L; // Mock tạm

        String paymentUrl = paymentService.createPaymentUrl(
                mockEmployerId, 
                requestDTO.getPackageId(), 
                requestDTO.getAmount(), 
                request
        );

        return ResponseEntity.ok(new PaymentResponseDTO(paymentUrl));
    }

    // API 2: Webhook dành cho Server VNPay gọi ngầm về (Chống hack/bug)
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIPN(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        Map<String, String> result = paymentService.processVnPayIPN(fields);
        return ResponseEntity.ok(result);
    }
}