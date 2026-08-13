package ru.yandex.practicum.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "records")
public class InventoryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    Long productId;

    Integer quantity;

    @Column(name = "reserved_quantity")
    Integer reservedQuantity = 0;

    @Column(name = "available_quantity")
    Integer availableQuantity = 0;

    @Version
    private Long version;
}
