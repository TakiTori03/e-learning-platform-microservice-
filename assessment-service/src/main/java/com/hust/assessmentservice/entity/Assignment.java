package com.hust.assessmentservice.entity;

import com.hust.assessmentservice.entity.enums.TargetType;
import com.hust.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "assignments", indexes = {
    @Index(name = "idx_assignment_target", columnList = "target_id, target_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends BaseEntity<String> {

    @PrePersist
    public void ensureId() {
        if (this.getId() == null) {
            this.setId(java.util.UUID.randomUUID().toString());
        }
    }

    private String title;

    @Column(name = "course_id")
    private String courseId;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "target_id")
    private String targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private TargetType targetType;

    @Column(name = "attachment_file_key")
    private String attachmentFileKey;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "max_points")
    private Double maxPoints;
}
