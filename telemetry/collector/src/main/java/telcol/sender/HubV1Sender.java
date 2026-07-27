package telcol.sender;

import org.springframework.stereotype.Component;
import telcol.config.KafkaProperties;
import tools.producer.ProducerClient;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import tools.sender.BaseSender;

@Component
public class HubV1Sender extends BaseSender<HubEventAvro> {

    public HubV1Sender(ProducerClient client, KafkaProperties properties) {
        super(client, properties.getTopics().getHubs());
    }
}