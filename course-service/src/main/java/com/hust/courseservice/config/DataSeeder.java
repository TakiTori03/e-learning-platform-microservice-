package com.hust.courseservice.config;

import com.hust.courseservice.entity.Category;
import com.hust.courseservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    @Bean
    CommandLineRunner initCategories(CategoryRepository categoryRepository) {
        return args -> {
            if (categoryRepository.count() == 0) {
                log.info("Seeding initial categories into MongoDB...");
                categoryRepository.saveAll(List.of(
                    Category.builder().name("Web Development").categorySlug("web-development").icon("code").build(),
                    Category.builder().name("Mobile App").categorySlug("mobile-app").icon("mobile").build(),
                    Category.builder().name("Design").categorySlug("design").icon("paint-brush").build(),
                    Category.builder().name("Business").categorySlug("business").icon("briefcase").build(),
                    Category.builder().name("Marketing").categorySlug("marketing").icon("bullhorn").build()
                ));
                log.info("Seeding completed.");
            }
        };
    }
}
