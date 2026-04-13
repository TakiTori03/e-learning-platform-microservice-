package com.hust.courseservice.dto.response;

import com.hust.commonlibrary.dto.TimeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

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
    private String instructorId;
    private String categoryName;
    private List<String> requirements;
    private List<String> willLearns;
    private List<String> tags;
}
