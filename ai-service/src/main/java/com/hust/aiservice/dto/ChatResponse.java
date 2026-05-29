package com.hust.aiservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String sessionId; // Gửi lại để frontend lưu cho các lượt tiếp theo
    private String answer;
    private List<Citation> citations;
}
