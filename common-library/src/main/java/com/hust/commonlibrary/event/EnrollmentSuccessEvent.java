package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentSuccessEvent {
    private String orderId;
    private String userId;
    private String status; // Ví dụ: SUCCESS
}
