package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.ProductMapper;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public List<ProductDto> getProducts() {
        List<Product> productList = productRepository.findAllByActiveIsTrue();

        log.debug("Get products returns list with {} active values", productList.size());

        if (productList.isEmpty()) {
            return new ArrayList<>();
        }

        return productList.stream()
                .map(ProductMapper::mapToDto)
                .toList();
    }

    public ProductDto getProductById(long id) {
        Product product = productRepository.findByIdAndActiveIsTrue(id)
                .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));

        log.debug("Get product by id returns: {}", product);

        return ProductMapper.mapToDto(product);
    }

    public List<ProductDto> getProductsByCategoryId(long categoryId) {
        List<Product> productList = productRepository.findAllByCategoryIdAndActiveIsTrue(categoryId);

        log.debug("Get products by categoryId: {} returns list with {} active values", categoryId, productList.size());

        if (productList.isEmpty()) {
            return new ArrayList<>();
        }

        return productList.stream()
                .map(ProductMapper::mapToDto)
                .toList();
    }

    public List<ProductDto> searchProductsByNameLike(String query) {
        List<Product> productList = productRepository.findByNameContainingIgnoreCase(query);

        log.debug("Get products found by key-word: {} returns list with {} active values", query, productList.size());

        if (productList.isEmpty()) {
            return new ArrayList<>();
        }

        return productList.stream()
                .map(ProductMapper::mapToDto)
                .toList();
    }

    @Transactional
    public ProductDto addProduct(CreateProductRequest request) {
        log.debug("Post add product request with: {}", request);

        Category category = categoryService.getCategoryByIdOrElseThrow(request.categoryId());
        log.debug("Category found: {}", category);

        Product newProduct = ProductMapper.mapToProduct(request, category);

        newProduct = productRepository.save(newProduct);
        log.debug("Product mapped and saved to DB with data: {}", newProduct);

        return ProductMapper.mapToDto(newProduct);
    }

    @Transactional
    public ProductDto updateProduct(long id, UpdateProductRequest request) {
        log.debug("Patch update product request with: {}", request);

        Product updatedProduct = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product with id " + id + " not found"));
        log.debug("Product found id DB: {}", updatedProduct);

        if (request.categoryId() == null) {
            ProductMapper.mapToProduct(updatedProduct, request);
        } else {
            Category category = categoryService.getCategoryByIdOrElseThrow(request.categoryId());
            ProductMapper.mapToProduct(updatedProduct, request, category);
        }

        updatedProduct = productRepository.save(updatedProduct);
        log.debug("Product updated fields and saved to DB with data: {}", updatedProduct);

        return ProductMapper.mapToDto(updatedProduct);
    }
}
