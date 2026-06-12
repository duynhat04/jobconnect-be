package com.jobconnect.dto;

import com.jobconnect.entity.EmploymentType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class JobRequest {

    @NotBlank(message = "Tiêu đề công việc không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Địa điểm làm việc không được để trống")
    private String location;

    private Long salary;

    private Long companyId;

    private String requirements;

    private String category;

    @NotNull(message = "Hình thức tuyển dụng không được để trống")
    private EmploymentType employmentType;

    @NotNull(message = "Ngày hết hạn không được để trống")
    @Future(message = "Ngày hết hạn phải lớn hơn ngày hiện tại")
    private LocalDate expiredAt;
}