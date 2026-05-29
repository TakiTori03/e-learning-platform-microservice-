package com.hust.aiservice.repository;

import com.hust.aiservice.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByUserIdAndCourseIdOrderByUpdatedAtDesc(String userId, String courseId);
}
