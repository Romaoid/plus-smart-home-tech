package telcol.mapper;

import telcol.dto.hub.device.added.DeviceAddedEvent;
import telcol.dto.hub.device.removed.DeviceRemovedEvent;
import telcol.dto.hub.scenario.added.ScenarioAddedEvent;
import telcol.dto.hub.scenario.removed.ScenarioRemovedEvent;
import ru.yandex.practicum.kafka.telemetry.event.*;

public class HubMapper {
    public static HubEventAvro mapToAvro(DeviceAddedEvent e) {
        DeviceAddedEventAvro event = DeviceAddedEventAvro.newBuilder()
                .setId(e.getId())
                .setType(
                        DeviceTypeAvro.valueOf(
                                e.getDeviceType().toString()))
                .build();

        return HubEventAvro.newBuilder()
                .setHubId(e.getHubId())
                .setTimestamp(e.getTimestamp())
                .setPayload(event)
                .build();
    }

    public static HubEventAvro mapToAvro(DeviceRemovedEvent e) {
        DeviceRemovedEventAvro event = DeviceRemovedEventAvro.newBuilder()
                .setId(e.getId())
                .build();

        return HubEventAvro.newBuilder()
                .setHubId(e.getHubId())
                .setTimestamp(e.getTimestamp())
                .setPayload(event)
                .build();
    }

    public static HubEventAvro mapToAvro(ScenarioAddedEvent e) {
        ScenarioAddedEventAvro event = ScenarioAddedEventAvro.newBuilder()
                .setName(e.getName())
                .setConditions(
                        e.getConditions().stream()
                        .map(c -> ScenarioConditionAvro.newBuilder()
                                .setSensorId(c.getSensorId())
                                .setType(ConditionTypeAvro.valueOf(c.getType().toString()))
                                .setOperation(ConditionOperationAvro.valueOf(c.getOperation().toString()))
                                .setValue(c.getValue()).build())
                        .toList()
                )
                .setActions(
                        e.getActions().stream()
                        .map(a -> DeviceActionAvro.newBuilder()
                                .setSensorId(a.getSensorId())
                                .setType(ActionTypeAvro.valueOf(a.getType().toString()))
                                .setValue(a.getValue())
                                .build())
                        .toList()
                )
                .build();

        return HubEventAvro.newBuilder()
                .setHubId(e.getHubId())
                .setTimestamp(e.getTimestamp())
                .setPayload(event)
                .build();
    }

    public static HubEventAvro mapToAvro(ScenarioRemovedEvent e) {
        ScenarioRemovedEventAvro event = ScenarioRemovedEventAvro.newBuilder()
                .setName(e.getName())
                .build();

        return HubEventAvro.newBuilder()
                .setHubId(e.getHubId())
                .setTimestamp(e.getTimestamp())
                .setPayload(event)
                .build();
    }
}
