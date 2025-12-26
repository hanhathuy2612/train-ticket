package com.example.notificationservice.dto;

import com.example.notificationservice.entity.NotificationPreference;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationPreferenceResponse {
    
    private Long userId;
    private EmailPreferences email;
    private SmsPreferences sms;
    private PushPreferences push;
    private QuietHours quietHours;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailPreferences {
        private Boolean enabled;
        private Boolean bookingConfirmation;
        private Boolean paymentNotification;
        private Boolean tripReminder;
        private Boolean promotional;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsPreferences {
        private Boolean enabled;
        private Boolean bookingConfirmation;
        private Boolean tripReminder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PushPreferences {
        private Boolean enabled;
        private Boolean bookingUpdate;
        private Boolean scheduleChange;
        private Boolean promotional;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuietHours {
        private Integer start;
        private Integer end;
    }

    public static NotificationPreferenceResponse from(NotificationPreference pref) {
        return NotificationPreferenceResponse.builder()
                .userId(pref.getUserId())
                .email(EmailPreferences.builder()
                        .enabled(pref.getEmailEnabled())
                        .bookingConfirmation(pref.getEmailBookingConfirmation())
                        .paymentNotification(pref.getEmailPaymentNotification())
                        .tripReminder(pref.getEmailTripReminder())
                        .promotional(pref.getEmailPromotional())
                        .build())
                .sms(SmsPreferences.builder()
                        .enabled(pref.getSmsEnabled())
                        .bookingConfirmation(pref.getSmsBookingConfirmation())
                        .tripReminder(pref.getSmsTripReminder())
                        .build())
                .push(PushPreferences.builder()
                        .enabled(pref.getPushEnabled())
                        .bookingUpdate(pref.getPushBookingUpdate())
                        .scheduleChange(pref.getPushScheduleChange())
                        .promotional(pref.getPushPromotional())
                        .build())
                .quietHours(QuietHours.builder()
                        .start(pref.getQuietHoursStart())
                        .end(pref.getQuietHoursEnd())
                        .build())
                .build();
    }
}

