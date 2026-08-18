package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryRecord;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public List<InventoryDto> getRecords() {
        List<InventoryRecord> recordList = inventoryRepository.findAll();

        log.debug("Get records returns list with {} values", recordList.size());

        return recordList.isEmpty()
                ? new ArrayList<>()
                : recordList.stream()
                .map(InventoryMapper::mapToDto)
                .toList();
    }

    public InventoryDto getRecordByProductId(long id) {
        InventoryRecord record = inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new NotFoundException("Record with product id " + id + " not found"));

        log.debug("Get record by product id returns: {}", record);

        return InventoryMapper.mapToDto(record);
    }

    @Transactional
    public InventoryDto addInventoryRecord(UpdateInventoryRequest request) {
        log.debug("Post add record request with: {}", request);

        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new IllegalArgumentException("Record with product id: " + request.productId() + "already exist");
        }

        InventoryRecord newRecord = InventoryMapper.mapToRecordFromCreateReq(request);
        calculateAvailableQuantity(newRecord);

        newRecord = inventoryRepository.save(newRecord);
        log.debug("Product mapped and saved to DB with data: {}", newRecord);

        return InventoryMapper.mapToDto(newRecord);
    }

    @Transactional
    public InventoryDto updateInventoryRecord(UpdateInventoryRequest request) {
        log.debug("Put update record request with: {}", request);
        InventoryRecord oldRecord;
        InventoryRecord updatedRecord;

        oldRecord = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new NotFoundException("Record with product id " + request.productId() + " not found"));
        log.debug("Record found id DB: {}", oldRecord);

        validateUpdateQuantity(oldRecord.getReservedQuantity(), request.quantity());

        updatedRecord = InventoryMapper.mapToRecordFromUpdateReq(oldRecord, request);
        calculateAvailableQuantity(updatedRecord);

        updatedRecord = inventoryRepository.save(updatedRecord);
        log.debug("Record updated fields and saved to DB with data: {}", updatedRecord);

        return InventoryMapper.mapToDto(updatedRecord);
    }

    @Transactional
    public ReserveResponse addReserve(ReserveRequest request) {
        log.debug("Post reserve quantity request with: {}", request);
        InventoryRecord oldRecord;
        InventoryRecord updatedRecord;

        oldRecord = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new NotFoundException("Record with product id " + request.productId() + " not found"));
        log.debug("Record found id DB: {}", oldRecord);

        try {
            validateAvailableQuantity(oldRecord.getAvailableQuantity(), request.quantity());

            updatedRecord = InventoryMapper.mapToRecordFromReserveReq(oldRecord, request);
            calculateAvailableQuantity(updatedRecord);

            updatedRecord = inventoryRepository.save(updatedRecord);
            log.debug("Record updated fields and saved to DB with data: {}", updatedRecord);

            return InventoryMapper.mapToResponse(true, updatedRecord.getAvailableQuantity(), "Reservation is success");
        } catch (InsufficientStockException e) {
            log.warn("Update rejected: {}", e.getMessage());
            return InventoryMapper.mapToResponse(false, oldRecord.getAvailableQuantity(), e.getMessage());
        }
    }

    @Transactional
    public ReserveResponse removeReserve(ReserveRequest request) {
        log.debug("Post release quantity request with: {}", request);
        InventoryRecord oldRecord;
        InventoryRecord updatedRecord;

        oldRecord = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new NotFoundException("Record with product id " + request.productId() + " not found"));
        log.debug("Record found id DB: {}", oldRecord);

        try {
            validateReserveQuantity(oldRecord.getReservedQuantity(), request.quantity());

            updatedRecord = InventoryMapper.mapToRecordFromRemoveReq(oldRecord, request);
            calculateAvailableQuantity(updatedRecord);

            updatedRecord = inventoryRepository.save(updatedRecord);
            log.debug("Record updated fields and saved to DB with data: {}", updatedRecord);

            return InventoryMapper.mapToResponse(true, updatedRecord.getAvailableQuantity(), "Release is success");
        } catch (InsufficientStockException e) {
            log.warn("Update rejected: {}", e.getMessage());
            return InventoryMapper.mapToResponse(false, oldRecord.getAvailableQuantity(), e.getMessage());
        }
    }

    private void validateAvailableQuantity(int availableQuantity, int quantityToReserve) {
        if (quantityToReserve > availableQuantity) {
            throw new InsufficientStockException("Fault because reserve quantity(" + quantityToReserve + ") more than available");
        }
    }

    private void validateUpdateQuantity(int reservedQuantity, int newQuantity) {
        if (reservedQuantity > newQuantity) {
            throw new InsufficientStockException("Fault because reserve quantity(" +
                    reservedQuantity + ") more than value quantity to update " + newQuantity);
        }
    }

    private void calculateAvailableQuantity(InventoryRecord record) {
        if (record.getReservedQuantity() == null) {
            record.setAvailableQuantity(record.getQuantity());
        } else {
            record.setAvailableQuantity(record.getQuantity() - record.getReservedQuantity());
        }
    }

    private void validateReserveQuantity(int reservedQuantity, int quantityToRelease) {
        if (quantityToRelease > reservedQuantity) {
            throw new InsufficientStockException("Fault because release quantity(" + quantityToRelease + ") more than reserved");
        }
    }
}
