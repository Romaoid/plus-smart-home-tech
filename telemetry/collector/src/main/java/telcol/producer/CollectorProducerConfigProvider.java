package telcol.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import telcol.config.KafkaProperties;
import tools.producer.ProducerConfigProvider;

@Component
@RequiredArgsConstructor
public class CollectorProducerConfigProvider implements ProducerConfigProvider {

    private final KafkaProperties kafkaProperties;

    @Override
    public String getBootstrapServers() {
        return kafkaProperties.getProducer().getBootstrapServers();
    }

    @Override
    public String getKeySerializer() {
        return kafkaProperties.getProducer().getKeySerializer();
    }

    @Override
    public String getValueSerializer() {
        return kafkaProperties.getProducer().getValueSerializer();
    }
}
