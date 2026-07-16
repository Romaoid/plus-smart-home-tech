package telcol.sender;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import telcol.producer.ProducerClient;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Component
@Slf4j
public class HubV1Sender {
    private final ProducerClient client;

    private final String topic = EventTopics.TELEMETRY_HUBS_V1_TOPIC;

    public HubV1Sender(ProducerClient client) {
        this.client = client;
    }

    @PreDestroy
    public void stop() {
        client.stop();
    }

    public void sendEvent(HubEventAvro message) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, message);
        client.getProducer().send(record);
    }
}
