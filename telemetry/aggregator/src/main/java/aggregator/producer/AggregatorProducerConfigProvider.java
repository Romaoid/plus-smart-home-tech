package aggregator.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import aggregator.config.KafkaProperties;
import tools.producer.ProducerConfigProvider;

@Component
@RequiredArgsConstructor
public class AggregatorProducerConfigProvider implements ProducerConfigProvider {
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
