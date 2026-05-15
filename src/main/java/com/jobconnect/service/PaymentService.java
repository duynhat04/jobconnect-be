package com.jobconnect.service;

import com.jobconnect.entity.Transaction;
import com.jobconnect.repository.TransactionRepository;
import com.jobconnect.config.VNPayConfig; 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    // Tạm gọi EmployerService để cộng gói sau khi thanh toán thành công
    // private final EmployerService employerService; 

    @Transactional
    public String createPaymentUrl(Long employerId, Long packageId, BigDecimal amount, HttpServletRequest request) {
        // 1. Tạo mã giao dịch duy nhất
        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        
        // 2. Lưu giao dịch xuống DB với trạng thái PENDING
        Transaction txn = Transaction.builder()
                .employerId(employerId)
                .packageId(packageId)
                .amount(amount)
                .status("PENDING")
                .txnRef(vnp_TxnRef)
                .paymentMethod("VNPAY")
                .build();
        transactionRepository.save(txn);

        // 3. Build URL gửi sang VNPay
        long amountInVnPayFormat = amount.longValue() * 100; // VNPay yêu cầu nhân 100
        
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VNPayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", VNPayConfig.vnp_Command);
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVnPayFormat));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang: " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl); // Link Frontend trang Success
        vnp_Params.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        // Sắp xếp tham số và tạo chữ ký bảo mật
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        
        return VNPayConfig.vnp_PayUrl + "?" + queryUrl;
    }

    @Transactional
    public Map<String, String> processVnPayIPN(Map<String, String> params) {
        Map<String, String> response = new HashMap<>();
        try {
            String vnp_SecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHashType");
            params.remove("vnp_SecureHash");

            // Tạo lại Hash để so sánh
            String signValue = VNPayConfig.hashAllFields(params);
            
            if (signValue.equals(vnp_SecureHash)) {
                String txnRef = params.get("vnp_TxnRef");
                String responseCode = params.get("vnp_ResponseCode");

                Transaction txn = transactionRepository.findByTxnRef(txnRef).orElse(null);

                if (txn != null) {
                    if ("PENDING".equals(txn.getStatus())) {
                        if ("00".equals(responseCode)) {
                            // Thành công
                            txn.setStatus("SUCCESS");
                            // TODO: Mở khóa gói đăng tin / Cộng lượt cho Employer ở đây
                            // employerService.addPackage(txn.getEmployerId(), txn.getPackageId());
                        } else {
                            // Thất bại
                            txn.setStatus("FAILED");
                        }
                        transactionRepository.save(txn);
                        
                        response.put("RspCode", "00");
                        response.put("Message", "Confirm Success");
                    } else {
                        response.put("RspCode", "02");
                        response.put("Message", "Order already confirmed");
                    }
                } else {
                    response.put("RspCode", "01");
                    response.put("Message", "Order not found");
                }
            } else {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
            }
        } catch (Exception e) {
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        return response;
    }
}