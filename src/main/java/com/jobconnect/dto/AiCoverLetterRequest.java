package com.jobconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiCoverLetterRequest {
    
    @NotBlank(message = "Tên ứng viên không được để trống")
    private String candidateName;
    
    @NotBlank(message = "Kỹ năng của bạn không được để trống")
    private String skills;
    
    @NotBlank(message = "Tiêu đề công việc không được để trống")
    private String jobTitle;
    
    @NotBlank(message = "Tên công ty không được để trống")
    private String companyName;
}