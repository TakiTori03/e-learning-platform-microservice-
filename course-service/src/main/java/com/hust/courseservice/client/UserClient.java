package com.hust.courseservice.client;

import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.courseservice.client.dto.UserInternalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service")
public interface UserClient {

    @GetMapping("/internal/users/{id}")
    ApiResponse<UserInternalResponse> getUserById(@PathVariable("id") String id);

    @org.springframework.web.bind.annotation.PostMapping("/internal/users/bulk")
    ApiResponse<java.util.List<UserInternalResponse>> getUsersByIds(@org.springframework.web.bind.annotation.RequestBody java.util.List<String> ids);
}
