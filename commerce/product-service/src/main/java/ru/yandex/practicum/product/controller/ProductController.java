package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.service.ProductService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public List<ProductDto> getProducts() {
        return productService.getProducts();
    }

    @GetMapping("/{id}")
    public ProductDto getProductById(@PathVariable long id) {
        return productService.getProductById(id);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductDto> getProductsByCategoryId(@PathVariable long categoryId) {
        return productService.getProductsByCategoryId(categoryId);
    }

    @GetMapping("/search")
    public List<ProductDto> searchProductsByName(@RequestParam String query) {
        return productService.searchProductsByNameLike(query);
    }

    @PostMapping
    public ResponseEntity<ProductDto> addProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductDto responseBody = productService.addProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable long id,
                                                    @Valid @RequestBody UpdateProductRequest request) {
        ProductDto responseBody = productService.updateProduct(id, request);

        return ResponseEntity.status(HttpStatus.OK).body(responseBody);
    }
}
