package com.hust.orderservice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotEmpty
    private List<String> courseIds;
    
    private String note;
    private String couponCode;
    
    
    private String userName;
    private String userEmail;
}
