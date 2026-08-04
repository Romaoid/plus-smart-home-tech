package telcol.handler.hub;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import ru.yandex.practicum.kafka.telemetry.event.*;
import telcol.sender.HubV1Sender;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class ScenarioAddedEventHandler implements HubEventHandler {
    private HubV1Sender hubV1Sender;
    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.SCENARIO_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        ScenarioAddedEventProto scenarioAddProto = event.getScenarioAdded();

        List<ScenarioConditionAvro> conditionAvros = getConditionAvroList(scenarioAddProto.getConditionList());

        List<DeviceActionAvro> actionAvros = getActionAvroList(scenarioAddProto.getActionList());

        ScenarioAddedEventAvro scenarioAddedEventAvro = ScenarioAddedEventAvro.newBuilder()
                .setName(scenarioAddProto.getId())
                .setConditions(conditionAvros)
                .setActions(actionAvros)
                .build();

        log.debug("Отправляю: {}, {}",event, scenarioAddedEventAvro);
        hubV1Sender.sendEvent(
                build(event, scenarioAddedEventAvro));
    }

    private List<ScenarioConditionAvro> getConditionAvroList(List<ScenarioConditionProto> conditionProtoList) {
        return conditionProtoList.stream()
                .map(c -> ScenarioConditionAvro.newBuilder()
                        .setSensorId(c.getSensorId())
                        .setType(ConditionTypeAvro.valueOf(c.getType().toString()))
                        .setOperation(ConditionOperationAvro.valueOf(c.getOperation().toString()))
                        .setValue(
                                c.getValueCase() == ScenarioConditionProto.ValueCase.BOOL_VALUE
                                        ? c.getBoolValue()
                                        : c.getIntValue())
                        .build())
                .toList();
    }

    private List<DeviceActionAvro> getActionAvroList(List<DeviceActionProto> actionProtoList) {
        return actionProtoList.stream()
                .map(a -> DeviceActionAvro.newBuilder()
                        .setSensorId(a.getSensorId())
                        .setType(ActionTypeAvro.valueOf(a.getType().toString()))
                        .setValue(a.getValue())
                        .build())
                .toList();
    }
}
