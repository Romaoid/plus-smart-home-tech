package telcol.sender;

import org.springframework.stereotype.Component;
import telcol.config.KafkaProperties;
import telcol.producer.ProducerClient;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Component
public class HubV1Sender extends BaseEventSender<HubEventAvro>{

    public HubV1Sender(ProducerClient client, KafkaProperties properties) {
        super(client, properties.getTopics().getHubs());
    }
}