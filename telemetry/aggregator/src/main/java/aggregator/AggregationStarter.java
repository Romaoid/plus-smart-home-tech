package aggregator;

import aggregator.config.KafkaProperties;
import aggregator.consumer.AggregatorConsumerConfigProvider;
import aggregator.sender.SnapshotV1Sender;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import tools.consumer.BaseConsumer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class AggregationStarter {
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final Map<String, SensorsSnapshotAvro> snapshotMap = new HashMap<>();
    private final SnapshotV1Sender producer;
    private final Consumer<Void, SensorEventAvro> consumer;
    private final Duration consumeAttemptTimeout;
    private final List<String> consumerSensorsTopics;

    public AggregationStarter(SnapshotV1Sender producer,
                              BaseConsumer baseConsumer,
                              AggregatorConsumerConfigProvider consumerConfig,
                              KafkaProperties properties) {
        this.producer = producer;
        this.consumer = baseConsumer.create(consumerConfig);
        this.consumeAttemptTimeout = Duration.ofMillis(properties.getConsumerAttemptTimeout());
        this.consumerSensorsTopics = properties.getTopics().getSensorsList();
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(consumerSensorsTopics);
            log.info("Подписались на топики: {}", consumerSensorsTopics);

            while (true) {
                ConsumerRecords<Void, SensorEventAvro> records = consumer.poll(consumeAttemptTimeout);

                int count = 0;
                for (ConsumerRecord<Void, SensorEventAvro> record : records) {

                    Optional<SensorsSnapshotAvro> updatedSnapshot = updateState(record.value());

                    updatedSnapshot.ifPresent(producer::sendEvent);

                    manageOffsets(record, count, consumer);
                    count++;
                }

                consumer.commitAsync();
            }

        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {

            try {
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.stop();
            }
        }
    }

    private void manageOffsets(ConsumerRecord<Void, SensorEventAvro> record,
                                      int count, Consumer<Void, SensorEventAvro> consumer) {
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


    private Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        SensorsSnapshotAvro snapshotAvro;
        log.info("Проверка на наличие новых показаний из события HubID: {}, sensorId: {}, Event Time: {}, SensorData: {}",
                event.getHubId(), event.getId(), event.getTimestamp(), event.getPayload());

        if (snapshotMap.containsKey(event.getHubId())) {
            snapshotAvro = snapshotMap.get(event.getHubId());
        } else {
            log.info("Snapshot по HubID не найден");
            return Optional.of(addSnapshot(event));
        }

        Map<String, SensorStateAvro> sensorsState = snapshotAvro.getSensorsState();
        if (sensorsState.containsKey(event.getId())) {
            SensorStateAvro oldState = snapshotAvro.getSensorsState().get(event.getId());
            log.info("Показания из snapshot события EventID: {}, Event Time: {}, SensorData: {}",
                    event.getId(), oldState.getTimestamp(), oldState.getData());

            if (oldState.getTimestamp().isAfter(event.getTimestamp()) || oldState.getData().equals(event.getPayload())) {
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        sensorsState.put(event.getId(), newState);
        snapshotAvro.setTimestamp(event.getTimestamp());
        log.info("Обновленные показания из snapshot события EventID: {}, snapshot Time: {}, SensorData: {}",
                event.getId(), snapshotAvro.getTimestamp(), snapshotAvro.getSensorsState().get(event.getId()));
        return Optional.of(snapshotAvro);
    }

    private SensorsSnapshotAvro addSnapshot(SensorEventAvro event) {
        Map<String, SensorStateAvro> sensorsState = new HashMap<>();
        sensorsState.put(event.getId(), SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build());

        log.info("Создание нового snapshot для HubID: {}, snapshotState: {}",
                event.getHubId(), sensorsState);

        snapshotMap.put(event.getHubId(),
                SensorsSnapshotAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(Instant.now())
                .setSensorsState(sensorsState)
                .build()
        );

        return snapshotMap.get(event.getHubId());
    }
}
