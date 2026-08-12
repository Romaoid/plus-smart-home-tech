package ru.yandex.practicum.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Enumerated(value = EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "status_details")
    private String statusDetails;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();

    public void addItem(Item item) {
        items.add(item);
        item.setOrder(this);
    }
}
