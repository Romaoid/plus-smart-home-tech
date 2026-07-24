package telcol.handler.hub;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import telcol.sender.HubV1Sender;

@Component
@AllArgsConstructor
public class DeviceRemovedEventHandler implements HubEventHandler {
    private HubV1Sender hubV1Sender;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_REMOVED;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceRemovedEventAvro removedEventAvro = DeviceRemovedEventAvro.newBuilder()
                .setId(
                        event.getDeviceRemoved().getId())
                .build();

        hubV1Sender.sendEvent(
                build(event, removedEventAvro));
    }
}
