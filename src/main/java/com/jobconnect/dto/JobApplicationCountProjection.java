package com.jobconnect.dto;

public interface JobApplicationCountProjection {

    Long getJobId();

    Long getApplicationCount();

    Long getPendingApplicationCount();

    Long getReviewedApplicationCount();

    Long getAcceptedApplicationCount();

    Long getRejectedApplicationCount();
}