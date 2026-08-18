package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.logger.Loggable;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;

    @Loggable
    public List<OrderDto> getOrders() {
        List<Order> orders = orderRepository.findAll();

        return orders.isEmpty()
                ? new ArrayList<>()
                : orders.stream()
                .map(OrderMapper::mapToDto)
                .toList();
    }

    @Loggable
    public OrderDto getOrderById(long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order with id " + id + " not found"));

        return OrderMapper.mapToDto(order);
    }

    @Loggable
    public List<OrderDto> getOrdersByEmail(String email) {
        List<Order> orders = orderRepository.findByCustomerEmail(email);

        if (orders.isEmpty()) {
            throw new NotFoundException("Orders created with email: " + email + " not found");
        }

        return orders.stream()
                .map(OrderMapper::mapToDto)
                .toList();
    }

    @Loggable
    @Transactional
    public OrderDto addOrder(Order order) {
        calculateTotalPrice(order);

        order = orderRepository.save(order);

        return OrderMapper.mapToDto(order);
    }

    private void calculateTotalPrice(Order order) {
        BigDecimal totalPrice = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(totalPrice);
    }
}
