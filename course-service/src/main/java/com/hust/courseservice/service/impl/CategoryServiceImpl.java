package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.courseservice.dto.request.CategoryRequest;
import com.hust.courseservice.dto.response.CategoryResponse;
import com.hust.courseservice.entity.Category;
import com.hust.courseservice.mapper.CategoryMapper;
import com.hust.courseservice.repository.CategoryRepository;
import com.hust.courseservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse create(CategoryRequest request) {
        Category category = categoryMapper.requestToEntity(request);
        category.setCategorySlug(toSlug(request.getName()));
        category = categoryRepository.save(category);
        return categoryMapper.entityToResponse(category);
    }

    @Override
    public List<CategoryResponse> getAll() {
        return categoryMapper.entityToResponse(categoryRepository.findAll());
    }

    @Override
    public CategoryResponse getById(String id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.CATEGORY,
                        AppConstants.Field_Constants.ID,
                        id));
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s+").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
