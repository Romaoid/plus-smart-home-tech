package ru.yandex.practicum.product.mapper;

import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;

public class CategoryMapper {
    public static CategoryDto mapToDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }

    public static Category mapToCategory(CreateCategoryRequest request) {
        Category newCategory = new Category();

        newCategory.setName(request.name());

        if (!(request.description() == null) && !(request.description().isBlank())) {
            newCategory.setDescription(request.description());
        }

        return newCategory;
    }
}
