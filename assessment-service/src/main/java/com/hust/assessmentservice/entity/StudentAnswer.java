package com.hust.assessmentservice.entity;

import com.hust.commonlibrary.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_answers", indexes = {
    @Index(name = "idx_answer_submission", columnList = "submission_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAnswer extends BaseEntity<String> {

    @PrePersist
    public void ensureId() {
        if (this.getId() == null) {
            this.setId(java.util.UUID.randomUUID().toString());
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @Column(name = "question_id")
    private String questionId;

    @Column(name = "option_id")
    private String optionId;
}
