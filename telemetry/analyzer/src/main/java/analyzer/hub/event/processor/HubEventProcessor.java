package analyzer.hub.event.processor;

import analyzer.config.KafkaProperties;
import analyzer.hub.event.processor.consumer.AnalyzerHubConsumerConfigProvider;
import analyzer.hub.event.processor.service.DeviceService;
import analyzer.hub.event.processor.service.ScenarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import tools.consumer.BaseConsumer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {
    private final DeviceService deviceService;
    private final ScenarioService scenarioService;
    private final KafkaProperties properties;
    private final AnalyzerHubConsumerConfigProvider consumerConfig;
    private final BaseConsumer baseConsumer;

    @Override
    public void run() {
        Consumer<Void, HubEventAvro> consumer = baseConsumer.create(consumerConfig);
        List<String> topics = properties.getTopics().getHubsList();
        Duration consumeAttemptTimeout = Duration.ofMillis(properties.getConsumerAttemptTimeout());

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(topics);

            while (true) {
                ConsumerRecords<Void, HubEventAvro> records = consumer.poll(consumeAttemptTimeout);

                if (records.isEmpty()) {
                    continue;
                }

                handleRecords(records);
                consumer.commitSync();
            }
        } catch (WakeupException ignores) {
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Consumer closing ");
                consumer.close();
            }
        }
    }

    private void handleRecords(ConsumerRecords<Void, HubEventAvro> records) {
        Map<Class<?>, List<HubEventAvro>> eventsMap =
                StreamSupport.stream(records.spliterator(), false)
                        .map(ConsumerRecord::value)
                        .filter(Objects::nonNull) // Защита от tombstone-сообщений (null-values)
                        .collect(Collectors.groupingBy(event -> event.getPayload().getClass()));

        eventsMap.forEach((c, e) ->
                log.info("Received {} events types: {}", e.size(), c.getSimpleName()));

        if (eventsMap.get(DeviceAddedEventAvro.class) != null
                && !eventsMap.get(DeviceAddedEventAvro.class).isEmpty()) {
            deviceService.addDevices(eventsMap.get(DeviceAddedEventAvro.class));
        }
        if (eventsMap.get(DeviceRemovedEventAvro.class) != null
                && !eventsMap.get(DeviceRemovedEventAvro.class).isEmpty()) {
            deviceService.removeDevices(eventsMap.get(DeviceRemovedEventAvro.class));
        }
        if (eventsMap.get(ScenarioAddedEventAvro.class) != null
                && !eventsMap.get(ScenarioAddedEventAvro.class).isEmpty()) {
            scenarioService.addScenarios(eventsMap.get(ScenarioAddedEventAvro.class));
        }
        if (eventsMap.get(ScenarioRemovedEventAvro.class) != null
                && !eventsMap.get(ScenarioRemovedEventAvro.class).isEmpty()) {
            scenarioService.removeScenarios(eventsMap.get(ScenarioRemovedEventAvro.class));
        }
    }
}
