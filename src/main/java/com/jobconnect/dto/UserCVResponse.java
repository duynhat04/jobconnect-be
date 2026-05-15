package com.jobconnect.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCVResponse {

    private Long id;
    private String cvName;
    private String fileUrl;
    private Boolean isDefault;
    private LocalDateTime uploadedAt;
}