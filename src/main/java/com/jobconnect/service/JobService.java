package com.jobconnect.service;

import com.jobconnect.dto.JobRequest;
import com.jobconnect.entity.Company;
import com.jobconnect.entity.Job;
import com.jobconnect.repository.CompanyRepository;
import com.jobconnect.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class JobService {
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CompanyRepository companyRepository;

    // 1. TẠO JOB MỚI (Dành cho Nhà tuyển dụng)
    public Job createJob(JobRequest jobRequest) {
        Company company = companyRepository.findById(jobRequest.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty này!"));

        // CHẶN BẢO VỆ: Công ty phải được duyệt thì mới có quyền đăng tin
        if (company.getStatus() == null || !company.getStatus().equals("APPROVED")) {
            throw new RuntimeException("Công ty của bạn chưa được xác thực. Vui lòng chờ Admin duyệt công ty trước!");
        }

        Job job = new Job();
        job.setTitle(jobRequest.getTitle());
        job.setDescription(jobRequest.getDescription());
        job.setLocation(jobRequest.getLocation());
        job.setSalary(jobRequest.getSalary());
        job.setCompany(company);
        job.setStatus("PENDING"); // Ép cứng trạng thái là Chờ duyệt

        return jobRepository.save(job);
    }

    // 2. ADMIN DUYỆT BÀI
    public Job approveJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng!"));
        job.setStatus("APPROVED");
        job.setRejectionReason(null); // Xóa lý do từ chối (nếu trước đó có)
        return jobRepository.save(job);
    }

    // 3. ADMIN TỪ CHỐI BÀI KÈM LÝ DO
    public Job rejectJob(Long jobId, String reason) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài đăng!"));
        job.setStatus("REJECTED");
        job.setRejectionReason(reason); // Lưu lại lý do từ chối để công ty đọc được
        return jobRepository.save(job);
    }

    // 4. Lấy TẤT CẢ Job (Dành cho Admin quản lý)
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    // 5. Lấy Job ĐÃ DUYỆT (Dành cho Ứng viên xem trên trang chủ)
    // Lấy Job ĐÃ DUYỆT (Dành cho Ứng viên xem trên trang chủ)
    public List<Job> getApprovedJobs() {
        // Chỉ lấy những job có trạng thái là APPROVED
        return jobRepository.findByStatus("APPROVED");
    }

    // Tìm kiếm, Lọc và Phân trang
    public Page<Job> searchJobs(String keyword, String location, Long minSalary, int page, int size) {
        // Tạo đối tượng phân trang (Trang bắt đầu từ 0), và sắp xếp theo ngày tạo giảm dần (mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return jobRepository.searchAndFilterJobs(keyword, location, minSalary, pageable);
    }
}