package ru.yandex.practicum.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.service.OrderOrchestrationService;
import ru.yandex.practicum.order.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderOrchestrationService orderOrchestrationService;

    @GetMapping
    public List<OrderDto> getOrders() {
        return orderService.getOrders();
    }

    @GetMapping("/{id}")
    public OrderDto getOrderById(@PathVariable long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/by-email")
    public List<OrderDto> getOrdersByEmail(@RequestParam String email) {
        return orderService.getOrdersByEmail(email);
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderDto body = orderOrchestrationService.createOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
