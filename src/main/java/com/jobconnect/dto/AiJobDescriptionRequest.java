package com.jobconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiJobDescriptionRequest {
    
    @NotBlank(message = "Tiêu đề công việc không được để trống")
    private String title;
    
    @NotBlank(message = "Yêu cầu kỹ năng không được để trống")
    private String skills;
    
    @NotBlank(message = "Địa điểm làm việc không được để trống")
    private String location;
}