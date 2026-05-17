package com.hust.commonlibrary.constant;

/**
 * Centralized Registry of all Apache Kafka Topic Names used across the microservices ecosystem.
 * Consolidating topics here prevents typo-driven producer/consumer mismatches.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Prevent instantiation
    }

    /**
     * Triggered when an Order is successfully paid in full.
     * Producer: order-service
     * Consumer: learning-service
     */
    public static final String ORDER_PAID = "order-paid-topic";

    /**
     * Callback triggered to signify successful enrollment fulfillment for the Saga pattern.
     * Producer: learning-service
     * Consumer: order-service
     */
    public static final String ENROLLMENT_SUCCESS = "enrollment-success-topic";

    /**
     * Triggered when a Lesson's material/content is updated.
     * Producer: course-service
     * Consumer: learning-service
     */
    public static final String LESSON_MATERIAL_UPDATED = "lesson-material-updated-topic";

    /**
     * Triggered when a student submits an assignment or completes an assessment.
     * Producer: assessment-service
     * Consumer: learning-service
     */
    public static final String ASSESSMENT_SUBMITTED = "assessment-submitted-topic";
}
