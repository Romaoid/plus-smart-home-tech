package ru.yandex.practicum.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.product.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByActiveIsTrue();

    Optional<Product> findByIdAndActiveIsTrue(long id);

    List<Product> findAllByCategoryIdAndActiveIsTrue(long categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);
}
