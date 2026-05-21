package com.hust.workerservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000); // 15s connection timeout để tránh lỗi khi parser tải file
        factory.setReadTimeout(90000);    // 90s read timeout (an toàn cho các tác vụ OCR bằng CPU/GPU tải nặng)
        return new RestTemplate(factory);
    }
}
