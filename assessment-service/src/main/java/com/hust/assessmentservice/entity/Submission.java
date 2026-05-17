package com.hust.assessmentservice.entity;

import com.hust.assessmentservice.entity.enums.SubmissionStatus;
import com.hust.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissions", indexes = {
    @Index(name = "idx_submission_user_quiz", columnList = "user_id, quiz_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission extends BaseEntity<String> {

    @PrePersist
    public void ensureId() {
        if (this.getId() == null) {
            this.setId(java.util.UUID.randomUUID().toString());
        }
    }

    @Column(name = "quiz_id")
    private String quizId;

    @Column(name = "user_id")
    private String userId;

    private Double score;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudentAnswer> answers = new ArrayList<>();
}
