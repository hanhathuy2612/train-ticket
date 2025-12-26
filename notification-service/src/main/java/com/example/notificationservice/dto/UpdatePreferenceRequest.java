package com.example.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferenceRequest {
    
    // Email preferences
    private Boolean emailEnabled;
    private Boolean emailBookingConfirmation;
    private Boolean emailPaymentNotification;
    private Boolean emailTripReminder;
    private Boolean emailPromotional;

    // SMS preferences
    private Boolean smsEnabled;
    private Boolean smsBookingConfirmation;
    private Boolean smsTripReminder;

    // Push notification preferences
    private Boolean pushEnabled;
    private Boolean pushBookingUpdate;
    private Boolean pushScheduleChange;
    private Boolean pushPromotional;

    // Quiet hours
    private Integer quietHoursStart;
    private Integer quietHoursEnd;
}

