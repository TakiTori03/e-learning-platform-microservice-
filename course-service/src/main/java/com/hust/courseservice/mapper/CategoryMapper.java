package com.hust.courseservice.mapper;

import com.hust.commonlibrary.mapper.BaseMapper;
import com.hust.courseservice.dto.request.CategoryRequest;
import com.hust.courseservice.dto.response.CategoryResponse;
import com.hust.courseservice.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper extends BaseMapper<Category, CategoryRequest, CategoryResponse> {

    @Override
    @Mapping(target = "categorySlug", ignore = true)
    Category requestToEntity(CategoryRequest request);

    @Override
    @Mapping(target = "slug", source = "categorySlug")
    CategoryResponse entityToResponse(Category entity);
}
