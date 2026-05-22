package com.jobconnect.service;

import com.jobconnect.exception.AIException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIRateLimitService {

    private static final int MAX_REQUESTS_PER_DAY = 20;

    private final Map<String, UsageInfo> usageMap = new ConcurrentHashMap<>();

    public void checkLimit(String key) {
        LocalDate today = LocalDate.now();

        UsageInfo usageInfo = usageMap.get(key);

        if (usageInfo == null || !usageInfo.date.equals(today)) {
            usageMap.put(key, new UsageInfo(today, 1));
            return;
        }

        if (usageInfo.count >= MAX_REQUESTS_PER_DAY) {
            throw new AIException(
                    "Bạn đã vượt quá số lần sử dụng AI hôm nay. Vui lòng thử lại vào ngày mai.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        usageInfo.count++;
    }

    private static class UsageInfo {
        private LocalDate date;
        private int count;

        public UsageInfo(LocalDate date, int count) {
            this.date = date;
            this.count = count;
        }
    }
}