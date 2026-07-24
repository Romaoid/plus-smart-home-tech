package telcol.handler.hub;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import telcol.sender.HubV1Sender;

@Component
@AllArgsConstructor
public class ScenarioRemovedEventHandler implements HubEventHandler {
    private HubV1Sender hubV1Sender;

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_REMOVED;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioRemovedEventAvro scenarioRemovedEventAvro = ScenarioRemovedEventAvro.newBuilder()
                .setName(
                        event.getDeviceRemoved().getId())
                .build();

        hubV1Sender.sendEvent(
                build(event, scenarioRemovedEventAvro));
    }
}
