package com.jobconnect.config;

import com.jobconnect.entity.User;
import com.jobconnect.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Chặn lại hỏi vé (Lấy Token từ Header của Request)
        String header = request.getHeader("Authorization");
        String token = null;
        String email = null;

        // Thẻ thật luôn bắt đầu bằng chữ "Bearer " (chuẩn quốc tế)
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7); // Cắt bỏ chữ "Bearer " để lấy lõi thẻ
            try {
                email = jwtUtils.getEmailFromToken(token); // Giải mã lấy email
            } catch (Exception e) {
                System.out.println("Lỗi Token hoặc Token đã hết hạn!");
            }
        }

        // 2. Nếu vé chuẩn (có email) và hệ thống chưa ghi nhận người này
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtils.validateToken(token)) {

                // Vào DB tìm xem ông này là ai
                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null) {

                    // --- ĐÂY LÀ PHẦN CODE ĐÃ ĐƯỢC SỬA SANG CÁCH 2 ---
                    // Lấy Role từ Database và gán vào danh sách Quyền (Authorities)
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (user.getRole() != null) {
                        authorities.add(new SimpleGrantedAuthority(user.getRole()));
                    }
                    // ------------------------------------------------

                    // Mở barie, cấp quyền cho đi tiếp vào bên trong hệ thống KÈM THEO ROLE
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        // 3. Cho phép request đi tiếp
        filterChain.doFilter(request, response);
    }
}