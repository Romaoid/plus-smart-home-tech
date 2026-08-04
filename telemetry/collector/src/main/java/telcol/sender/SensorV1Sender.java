package telcol.sender;

import org.springframework.stereotype.Component;
import telcol.config.KafkaProperties;
import tools.producer.ProducerClient;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import tools.sender.BaseSender;

@Component
public class SensorV1Sender extends BaseSender<SensorEventAvro> {

    public SensorV1Sender(ProducerClient client, KafkaProperties properties) {
        super(client, properties.getTopics().getSensors());
    }
}