package com.hust.aiservice.controller;

import com.hust.aiservice.dto.ChatRequest;
import com.hust.aiservice.dto.ChatResponse;
import com.hust.aiservice.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RagService ragService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        log.info("📩 Nhận yêu cầu chat RAG cho Course [{}]: {}", request.getCourseId(), request.getMessage());
        ChatResponse response = ragService.chatWithKnowledge(request.getCourseId(), request.getMessage());
        return ResponseEntity.ok(response);
    }
}
