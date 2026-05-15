package com.jobconnect.config;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VNPayConfig {
    
    // 1. CẤU HÌNH THÔNG TIN TÀI KHOẢN VNPAY (Thay bằng thông tin thật của sếp)
    public static String vnp_TmnCode = "YOUR_TMN_CODE"; // VD: "2X0B1123"
    public static String secretKey = "YOUR_HASH_SECRET"; // VD: "EJDKJNDSDKJ..."

    // 2. URL MÔI TRƯỜNG TEST (Sandbox)
    public static String vnp_PayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    
    // 3. NƠI VNPAY SẼ REDIRECT VỀ SAU KHI THANH TOÁN (Trang của Next.js)
    public static String vnp_ReturnUrl = "http://localhost:3000/payment/result"; 
    
    // 4. THÔNG SỐ MẶC ĐỊNH KHÔNG ĐỔI
    public static String vnp_Version = "2.1.0";
    public static String vnp_Command = "pay";

    // =======================================================================
    // DƯỚI NÀY LÀ CÁC HÀM TIỆN ÍCH (UTILITY) ĐỂ MÃ HÓA VÀ TẠO CHỮ KÝ 
    // Sếp cứ để nguyên, không cần sửa gì cả!
    // =======================================================================

    // Hàm tạo chuỗi bảo mật HMAC SHA512
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKeySpec = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    // Hàm băm tất cả các tham số để tạo chữ ký xác minh
    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        return hmacSHA512(secretKey, sb.toString());
    }

    // Lấy IP của người dùng để gửi cho VNPay
    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP";
        }
        return ipAdress;
    }

    // Tạo mã đơn hàng ngẫu nhiên (8 số)
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}