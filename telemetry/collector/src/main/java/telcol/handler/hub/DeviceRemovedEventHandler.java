package telcol.handler.hub;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import telcol.sender.HubV1Sender;

@Component
@AllArgsConstructor
@Slf4j
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

        log.debug("Отправляю: {}, {}",event, removedEventAvro);
        hubV1Sender.sendEvent(
                build(event, removedEventAvro));
    }
}
