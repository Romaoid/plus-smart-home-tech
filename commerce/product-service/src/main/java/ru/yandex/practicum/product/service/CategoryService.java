package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.CategoryMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getCategories() {
        List<Category> categoryList = categoryRepository.findAll();

        log.debug("Get categories returns list with {} values", categoryList.size());

        return categoryList.stream()
                .map(CategoryMapper::mapToDto)
                .toList();
    }

    public CategoryDto getCategoryById(long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category with id " + id + " not found"));

        log.debug("Get category by id returns: {}", category);

        return CategoryMapper.mapToDto(category);
    }

    @Transactional
    public CategoryDto addCategory(CreateCategoryRequest newCategory) {
        log.debug("Post add category requests with: {}", newCategory);

        Category category = CategoryMapper.mapToCategory(newCategory);

        category = categoryRepository.save(category);
        log.debug("Category mapped and saved to DB with data: {}", category);

        return CategoryMapper.mapToDto(category);
    }

    public Category getCategoryByIdOrElseThrow(long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category with id " + id + " not found"));
    }
}
