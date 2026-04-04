package com.jobconnect.dto;

public class ApplyExistingDto {
    private Long userCvId;
    private String coverLetter;

    // Getters and Setters
    public Long getUserCvId() { return userCvId; }
    public void setUserCvId(Long userCvId) {  this.userCvId = userCvId; }
    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
}