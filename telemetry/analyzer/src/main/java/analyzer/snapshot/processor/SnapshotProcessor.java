package analyzer.snapshot.processor;

import analyzer.hub.event.processor.entity.Condition;
import analyzer.hub.event.processor.entity.Scenario;
import analyzer.hub.event.processor.entity.ScenarioAction;
import analyzer.hub.event.processor.entity.ScenarioCondition;
import analyzer.hub.event.processor.service.ScenarioService;
import analyzer.config.KafkaProperties;
import analyzer.snapshot.processor.consumer.AnalyzerSnapshotConsumerConfigProvider;
import analyzer.snapshot.processor.grps.sender.AnalyzerSender;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.kafka.telemetry.event.*;
import tools.consumer.BaseConsumer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final AnalyzerSender gRpcSender;
    private final ScenarioService service;
    private final KafkaProperties properties;
    private final AnalyzerSnapshotConsumerConfigProvider consumerConfig;
    private final BaseConsumer baseConsumer;

    public void start() {
        Consumer<Void, SensorsSnapshotAvro> consumer = baseConsumer.create(consumerConfig);

        List<String> topics = properties.getTopics().getSnapshotsList();
        Duration consumeAttemptTimeout = Duration.ofMillis(properties.getConsumerAttemptTimeout());

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(topics);
            log.info("Subscribed to topics: {}", topics);

            while (true) {
                ConsumerRecords<Void, SensorsSnapshotAvro> records = consumer.poll(consumeAttemptTimeout);

                handleRecords(records, consumer);
            }
        } catch (WakeupException ignores) {
        } finally {
            try {
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Closing the consumer");
                consumer.close();
            }
        }
    }

    private void handleRecords(ConsumerRecords<Void, SensorsSnapshotAvro> records,
                              Consumer<Void, SensorsSnapshotAvro> consumer) {
        if (records.isEmpty()) {
            return;
        }
        log.info("Processing {} records", records.count());

        Map<String, SensorsSnapshotAvro> snapshotMap = new HashMap<>();
        int count = 0;

        for (ConsumerRecord<Void, SensorsSnapshotAvro> record : records) {
            if (record.value() == null) {
                log.warn("Received null snapshot, skipping");
                manageOffsets(record, count, consumer);
                count++;
                continue;
            }

            SensorsSnapshotAvro newSnapshot = record.value();
            String hubId = newSnapshot.getHubId();

            if (!snapshotMap.containsKey(hubId)
                    || newSnapshot.getTimestamp().isAfter(snapshotMap.get(hubId).getTimestamp())) {
                snapshotMap.put(hubId, newSnapshot);

                log.debug("Loading scenarios for hubId={}", hubId);
                List<Scenario> scenarios = service.getScenariosWithConditions(hubId);

                if (scenarios.isEmpty()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry sleep interrupted", e);
                    }

                    scenarios = service.getScenariosWithConditions(hubId);

                    if (scenarios.isEmpty()) {
                        log.debug("No scenarios for hubId={} after retry, skipping", hubId);
                        manageOffsets(record, count, consumer);
                        count++;
                        continue;
                    }
                }

                List<Scenario> matchedScenarios = getMatchedScenarios(scenarios, newSnapshot.getSensorsState());

                List<Scenario> matchedScenariosWithActions = getMatchedActions(matchedScenarios);

                if (matchedScenariosWithActions.isEmpty()) {
                    log.debug("No actions found for snapshot: {}", newSnapshot);
                    manageOffsets(record, count, consumer);
                    count++;
                    continue;
                }
                log.info("Found {} actions for scenarios", matchedScenariosWithActions.size());

                sendActions(hubId, matchedScenariosWithActions);

            } else {
                log.debug("Skipping older/duplicate snapshot for hubId={}, timestamp={}",
                        hubId, newSnapshot.getTimestamp());
            }

            manageOffsets(record, count, consumer);
            count++;
        }
    }

    private void manageOffsets(ConsumerRecord<Void, SensorsSnapshotAvro> record,
                               int count, Consumer<Void, SensorsSnapshotAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private List<Scenario> getMatchedScenarios(List<Scenario> scenarios,
                                               Map<String, SensorStateAvro> sensorsState) {
        if (scenarios == null || scenarios.isEmpty()) {
            log.debug("No scenarios found");
            return Collections.emptyList();
        }
        log.debug("Received {} scenarios from hubId", scenarios.size());

        List<Scenario> matched = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            if (checkAllConditions(scenario, sensorsState)) {
                matched.add(scenario);
            }
        }
        return matched;
    }

    private List<Scenario> getMatchedActions(List<Scenario> matched) {
        if (matched.isEmpty()) {
            log.debug("No scenarios found");

            return Collections.emptyList();
        }
        log.debug("Found {} matched scenarios", matched.size());

        Set<Long> matchedIds = matched.stream()
                .map(Scenario::getId)
                .collect(Collectors.toSet());
        log.debug("Loading {} actions for scenarios", matchedIds.size());

        return service.getScenariosWithActions(matchedIds);
    }

    private void sendActions(String hubId, List<Scenario> scenariosWithActions) {
        for (Scenario scenario : scenariosWithActions) {
            String scenarioName = scenario.getName();

            for (ScenarioAction sa : scenario.getActions()) {
                Instant now = Instant.now();
                Timestamp timestampNow = Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build();

                DeviceActionRequest request = DeviceActionRequest.newBuilder()
                        .setHubId(hubId)
                        .setScenarioName(scenarioName)
                        .setAction(getActionProto(sa))
                        .setTimestamp(timestampNow)
                        .build();

                try {
                    gRpcSender.send(request);
                } catch (Exception e) {
                    log.error("Failed to send actions for hubId={}", hubId, e);
                }
            }
        }
    }

    private DeviceActionProto getActionProto(ScenarioAction sa) {
        if (sa.getAction().getValue() == null) {
            return DeviceActionProto.newBuilder()
                    .setSensorId(sa.getSensor().getId())
                    .setType(ActionTypeProto.valueOf(
                            sa.getAction().getType().toString()))
                    .build();
        }
        return DeviceActionProto.newBuilder()
                .setSensorId(sa.getSensor().getId())
                .setType(ActionTypeProto.valueOf(
                        sa.getAction().getType().toString()))
                .setValue(sa.getAction().getValue())
                .build();
    }

    private boolean checkAllConditions(Scenario scenario,
                                       Map<String, SensorStateAvro> sensorsState) {
        Set<ScenarioCondition> conditions = scenario.getConditions();

        if (conditions == null || conditions.isEmpty()) {
            log.debug("Scenario {} has no conditions, skipping", scenario.getId());
            return false;
        }

        for (ScenarioCondition sc : conditions) {
            String sensorId = sc.getSensor().getId();
            Condition condition = sc.getCondition();

            SensorStateAvro sensorState = sensorsState.get(sensorId);
            if (sensorState == null) {
                log.debug("Sensor {} not found in snapshot for scenario {}",
                        sensorId, scenario.getId());
                return false;
            }

            boolean conditionMet = checkCondition(sensorState, condition);
            if (!conditionMet) {
                log.debug("Condition not met for sensor {} in scenario {}",
                        sensorId, scenario.getId());
                return false;
            }
        }

        return true;
    }

    private boolean checkCondition(SensorStateAvro sensorState, Condition condition) {
        Object sensorValue = extractSensorValue(sensorState, condition.getType());

        if (sensorValue == null) {
            log.debug("Could not extract value for condition type: {}", condition.getType());
            return false;
        }

        Integer threshold = condition.getValue();
        ConditionOperationAvro operation = condition.getOperation();

        return compareValues(sensorValue, threshold, operation);
    }

    private Object extractSensorValue(SensorStateAvro sensorState, ConditionTypeAvro conditionType) {
        Object data = sensorState.getData();

        if (data == null) {
            return null;
        }

        switch (conditionType) {
            case TEMPERATURE:
                if (data instanceof TemperatureSensorAvro) {
                    return ((TemperatureSensorAvro) data).getTemperatureC();
                }
                if (data instanceof ClimateSensorAvro) {
                    return ((ClimateSensorAvro) data).getTemperatureC();
                }
                break;

            case HUMIDITY:
                if (data instanceof ClimateSensorAvro) {
                    return ((ClimateSensorAvro) data).getHumidity();
                }
                break;

            case CO2LEVEL:
                if (data instanceof ClimateSensorAvro) {
                    return ((ClimateSensorAvro) data).getCo2Level();
                }
                break;

            case LUMINOSITY:
                if (data instanceof LightSensorAvro) {
                    return ((LightSensorAvro) data).getLuminosity();
                }
                break;

            case MOTION:
                if (data instanceof MotionSensorAvro) {
                    return ((MotionSensorAvro) data).getMotion();
                }
                break;

            case SWITCH:
                if (data instanceof SwitchSensorAvro) {
                    return ((SwitchSensorAvro) data).getState();
                }
                break;

            default:
                log.warn("Unsupported condition type: {}", conditionType);
                return null;
        }

        log.warn("Sensor data type {} does not match condition type {}",
                data.getClass().getSimpleName(), conditionType);
        return null;
    }

    private boolean compareValues(Object sensorValue, Integer threshold,
                                  ConditionOperationAvro operation) {
        switch (sensorValue) {
            case null -> {
                return false;
            }
            case Boolean boolValue -> {
                return switch (operation) {
                    case EQUALS -> boolValue == (threshold != null && threshold == 1);
                    case GREATER_THAN, LOWER_THAN -> {
                        log.warn("Boolean value with {} operation, skipping", operation);
                        yield false;
                    }
                    default -> false;
                };
            }
            case Integer intValue -> {
                if (threshold == null) {
                    log.warn("Threshold is null for integer comparison");
                    return false;
                }

                return switch (operation) {
                    case EQUALS -> intValue.equals(threshold);
                    case GREATER_THAN -> intValue > threshold;
                    case LOWER_THAN -> intValue < threshold;
                    default -> false;
                };
            }
            default -> {
            }
        }

        log.warn("Unsupported sensor value type: {}", sensorValue.getClass().getSimpleName());
        return false;
    }
}
