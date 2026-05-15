    package com.jobconnect.config;

    import org.springframework.security.core.Authentication;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.core.userdetails.UserDetails;

    public class SecurityUtils {

        public static String getCurrentUserEmail() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Chặn ngay nếu chưa đăng nhập hoặc lỗi Token (Fix NullPointerException)
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                throw new RuntimeException("Chưa xác thực người dùng hoặc Token không hợp lệ!");
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } 
            // Dự phòng trường hợp nó lưu thẳng chuỗi email hoặc Entity User (nếu có)
            return principal.toString();
        }
    }