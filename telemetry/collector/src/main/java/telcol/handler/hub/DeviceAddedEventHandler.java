package telcol.handler.hub;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import telcol.sender.HubV1Sender;

@Component
@AllArgsConstructor
public class DeviceAddedEventHandler implements HubEventHandler {
    private HubV1Sender hubV1Sender;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceAddedEventProto addedEventProto = event.getDeviceAdded();

        DeviceAddedEventAvro deviceAddedEventAvro = DeviceAddedEventAvro.newBuilder()
                .setId(addedEventProto.getId())
                .setType(
                        DeviceTypeAvro.valueOf(
                                addedEventProto.getType().toString()))
                .build();

        hubV1Sender.sendEvent(
                build(event, deviceAddedEventAvro));
    }
}
