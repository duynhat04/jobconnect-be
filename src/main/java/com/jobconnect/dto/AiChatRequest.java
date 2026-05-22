package com.jobconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String message;
}