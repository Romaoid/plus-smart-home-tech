package ru.yandex.practicum.product.mapper;

import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;

public class ProductMapper {
    public static ProductDto mapToDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                CategoryMapper.mapToDto(product.getCategory()),
                product.getImageUrl(),
                product.getActive()
        );
    }

    public static Product mapToProduct(CreateProductRequest request, Category category) {
        Product newProduct = new Product();

        newProduct.setName(request.name());

        newProduct.setPrice(request.price());

        newProduct.setCategory(category);

        newProduct.setActive(true);

        if (!(request.description() == null) && !(request.description().isBlank())) {
            newProduct.setDescription(request.description());
        }

        if (request.imageUrl() != null && !(request.imageUrl().isBlank())) {
            newProduct.setImageUrl(request.imageUrl());
        }

        return newProduct;
    }

    public static void mapToProduct(Product updatedProduct, UpdateProductRequest request) {
        mapToProduct(updatedProduct, request, null);
    }

    public static void mapToProduct(Product updatedProduct, UpdateProductRequest request, Category category) {
        if (!(request.name() == null) && !(request.name().isBlank())) {
            updatedProduct.setName(request.name());
        }

        if (!(request.price() == null)) {
            updatedProduct.setPrice(request.price());
        }

        if (!(category == null)) {
            updatedProduct.setCategory(category);
        }

        if (!(request.description() == null) && !(request.description().isBlank())) {
            updatedProduct.setDescription(request.description());
        }

        if (!(request.imageUrl() == null) && !(updatedProduct.getImageUrl().isBlank())) {
            updatedProduct.setImageUrl(request.imageUrl());
        }

        if (request.active() != null) {
            updatedProduct.setActive(request.active());
        }
    }
}
