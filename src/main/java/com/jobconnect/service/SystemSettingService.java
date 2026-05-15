package com.jobconnect.service;

import com.jobconnect.entity.SystemSetting;
import com.jobconnect.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingRepository settingRepository;

    // Lấy toàn bộ cấu hình trả về dạng Object (Map) cho Frontend dễ đọc
    public Map<String, String> getAllSettings() {
        List<SystemSetting> settings = settingRepository.findAll();
        
        // Convert List<SystemSetting> thành Map<String, String>
        return settings.stream()
                .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue));
    }

    // Nhận cục Object từ Frontend và lưu xuống DB
    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        // Duyệt qua từng Key-Value Frontend gửi lên
        newSettings.forEach((key, value) -> {
            // Tìm xem trong DB có key này chưa. Có thì update, chưa có thì tạo mới
            SystemSetting setting = settingRepository.findById(key)
                    .orElse(new SystemSetting(key, "", "")); // Default rỗng nếu chưa có
            
            setting.setValue(value);
            settingRepository.save(setting);
        });
    }
}