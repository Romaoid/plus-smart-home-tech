package aggregator.sender;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import aggregator.config.KafkaProperties;
import tools.producer.ProducerClient;
import tools.sender.BaseSender;

@Component
public class SnapshotV1Sender extends BaseSender<SensorsSnapshotAvro> {

    public SnapshotV1Sender(ProducerClient client, KafkaProperties properties) {
        super(client, properties.getTopics().getSnapshots());
    }
}
