package com.jobconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingApprovalDTO {

    private Long id;

    private String name;

    // COMPANY / JOB
    private String type;

    private String status;
}