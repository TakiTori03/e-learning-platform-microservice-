package com.hust.courseservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import com.hust.courseservice.client.dto.UserInternalResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse extends TimeResponse {
    private String id;
    private String code;
    private String name;
    private String subTitle;
    private String thumbnail;
    private String access;
    private String coursePreview;
    private Integer views;
    private Double price;
    private Double finalPrice;
    private String description;
    private String level;
    private String courseSlug;
    private String status;
    private String instructorId;
    
    private UserInternalResponse instructor;
    private CategoryResponse category;
    private List<SectionResponse> sections;

    private List<String> requirements;
    private List<String> willLearns;
    private List<String> tags;


    // 3. Số liệu thống kê (Tổng hợp từ các Service khác)
    private Integer sectionCount;      // Số lượng chương
    private Integer lessonCount;       // Tổng số bài học
    private Integer studentCount;      // Số học viên (Lấy từ Order-Service)
    private Double totalVideosLength;  // Tổng thời lượng video
    private Integer numOfReviews;      // Số đánh giá (Lấy từ Interaction-Service)
    private Double avgRatingStars;     // Điểm đánh giá (Lấy từ Interaction-Service)

    // 4. Trạng thái riêng của User gọi API
    private Boolean isBought;          // User đã mua chưa?
    private Double progress;           // Tiến độ học (0.0 đến 1.0)
}
