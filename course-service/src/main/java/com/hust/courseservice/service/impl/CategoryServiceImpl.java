package com.hust.courseservice.service.impl;

import com.hust.commonlibrary.constant.AppConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.courseservice.dto.request.CategoryRequest;
import com.hust.courseservice.dto.response.CategoryResponse;
import com.hust.courseservice.entity.Category;
import com.hust.courseservice.mapper.CategoryMapper;
import com.hust.courseservice.repository.CategoryRepository;
import com.hust.courseservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
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
    public CategoryResponse update(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.CATEGORY,
                        AppConstants.Field_Constants.ID,
                        id));

        categoryMapper.partialUpdate(category, request);
        category.setCategorySlug(toSlug(request.getName()));
        category = categoryRepository.save(category);
        return categoryMapper.entityToResponse(category);
    }

    @Override
    public void delete(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.CATEGORY,
                        AppConstants.Field_Constants.ID,
                        id));
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse detail(String id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::entityToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.CATEGORY,
                        AppConstants.Field_Constants.ID,
                        id));
    }

    @Override
    public ListResponse<CategoryResponse> search(String text, Pageable pageable) {
        Page<Category> categoryPage;
        if (text == null || text.trim().isEmpty()) {
            categoryPage = categoryRepository.findAll(pageable);
        } else {
            TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingAny(text);
            categoryPage = categoryRepository.findAllBy(criteria, pageable);
        }
        return ListResponse.of(categoryMapper.entityToResponse(categoryPage.getContent()), categoryPage);
    }

    @Override
    public List<CategoryResponse> getSelect() {
        return categoryMapper.entityToResponse(categoryRepository.findAll());
    }

    @Override
    public void updateStatus(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.CATEGORY,
                        AppConstants.Field_Constants.ID,
                        id));
        // Toggle or set status logic
        categoryRepository.save(category);
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = Pattern.compile("\\s+").matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
