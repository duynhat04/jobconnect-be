package com.jobconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiCvAnalysisRequest {

    @NotBlank(message = "Nội dung CV không được để trống")
    private String cvContent;

    private String targetJobTitle;
}