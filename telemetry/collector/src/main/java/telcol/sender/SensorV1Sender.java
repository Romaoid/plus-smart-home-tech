package telcol.sender;

import org.springframework.stereotype.Component;
import telcol.config.KafkaProperties;
import telcol.producer.ProducerClient;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class SensorV1Sender extends BaseEventSender<SensorEventAvro> {

    public SensorV1Sender(ProducerClient client, KafkaProperties properties) {
        super(client, properties.getTopics().getSensors());
    }
}