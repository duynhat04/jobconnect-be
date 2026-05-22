package com.jobconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiJobSuggestionRequest {

    @NotBlank(message = "Kỹ năng không được để trống")
    private String skills;

    @NotBlank(message = "Kinh nghiệm không được để trống")
    private String experience;

    @NotBlank(message = "Địa điểm mong muốn không được để trống")
    private String location;

    private String expectedSalary;
}