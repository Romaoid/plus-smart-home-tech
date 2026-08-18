package ru.yandex.practicum.inventory.mapper;

import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryRecord;

public class InventoryMapper {
    public static InventoryDto mapToDto(InventoryRecord record) {
        return new InventoryDto(
                record.getId(),
                record.getProductId(),
                record.getQuantity(),
                record.getReservedQuantity(),
                record.getAvailableQuantity()
        );
    }

    public static InventoryRecord mapToRecordFromCreateReq(UpdateInventoryRequest request) {
        InventoryRecord record = new InventoryRecord();

        record.setProductId(request.productId());
        record.setQuantity(request.quantity());

        return record;
    }

    public static InventoryRecord mapToRecordFromUpdateReq(InventoryRecord record,
                                                           UpdateInventoryRequest request) {
        record.setQuantity(request.quantity());

        return record;
    }

    public static InventoryRecord mapToRecordFromReserveReq(InventoryRecord record,
                                                           ReserveRequest request) {
        int reserved = record.getReservedQuantity() + request.quantity();

        record.setReservedQuantity(reserved);

        return record;
    }

    public static ReserveResponse mapToResponse(Boolean success,
                                                Integer availableQuantity,
                                                String message) {
        return new ReserveResponse(
                success,
                availableQuantity,
                message
        );
    }

    public static InventoryRecord mapToRecordFromRemoveReq(InventoryRecord record,
                                                            ReserveRequest request) {
        int reserved = record.getReservedQuantity() - request.quantity();

        record.setReservedQuantity(reserved);

        return record;
    }
}
