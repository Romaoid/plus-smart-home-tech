package ru.yandex.practicum.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.service.InventoryService;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public List<InventoryDto> getRecords() {
        return inventoryService.getRecords();
    }

    @GetMapping("/{productId}")
    public InventoryDto getRecordByProductId(@PathVariable long productId) {
        return inventoryService.getRecordByProductId(productId);
    }

    @PostMapping
    public ResponseEntity<InventoryDto> addInventoryRecord(@Valid @RequestBody UpdateInventoryRequest request) {
        InventoryDto responseBody = inventoryService.addInventoryRecord(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @PutMapping
    public InventoryDto updateInventoryRecord(@Valid @RequestBody UpdateInventoryRequest request) {
        return inventoryService.updateInventoryRecord(request);
    }

    @PostMapping("/reserve")
    public ResponseEntity<ReserveResponse> addReserve(@Valid @RequestBody ReserveRequest request) {
        ReserveResponse responseBody = inventoryService.addReserve(request);

        if (responseBody.success()) {
            return ResponseEntity.status(HttpStatus.OK).body(responseBody);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(responseBody);
        }
    }

    @PostMapping("/release")
    public ResponseEntity<ReserveResponse> removeReserve(@Valid @RequestBody ReserveRequest request) {
        ReserveResponse responseBody = inventoryService.removeReserve(request);

        if (responseBody.success()) {
            return ResponseEntity.status(HttpStatus.OK).body(responseBody);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
        }
    }
}
