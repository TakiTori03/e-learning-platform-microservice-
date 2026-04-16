package com.hust.courseservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.commonlibrary.mapper.GlobalMapperConfiguration;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.entity.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfiguration.class)
public interface CourseMapper extends BaseMapper<Course, CourseRequest, CourseResponse> {

    @Override
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "courseSlug", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "instructorId", ignore = true)
    Course requestToEntity(CourseRequest request);

    @Override
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "userId", source = "instructorId")
    @Mapping(target = "categoryId", source = "category.id")
    CourseResponse entityToResponse(Course entity);
}
