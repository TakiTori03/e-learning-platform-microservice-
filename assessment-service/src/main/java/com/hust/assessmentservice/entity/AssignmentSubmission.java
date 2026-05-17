package com.hust.assessmentservice.entity;

import com.hust.assessmentservice.entity.enums.SubmissionStatus;
import com.hust.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "assignment_submissions", indexes = {
    @Index(name = "idx_assignment_submission_user", columnList = "user_id, assignment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmission extends BaseEntity<String> {

    @PrePersist
    public void ensureId() {
        if (this.getId() == null) {
            this.setId(java.util.UUID.randomUUID().toString());
        }
    }

    @Column(name = "assignment_id")
    private String assignmentId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "submitted_file_url")
    private String submittedFileUrl;

    private Double grade;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}
