package aggregator.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.consumer.ConsumerConfigProvider;
import aggregator.config.KafkaProperties;

@Component
@RequiredArgsConstructor
public class AggregatorConsumerConfigProvider implements ConsumerConfigProvider {
    private final KafkaProperties kafkaProperties;

    @Override
    public String getClientId() {
        return kafkaProperties.getConsumer().getClientId();
    }

    @Override
    public String getGroupId() {
        return kafkaProperties.getConsumer().getGroupId();
    }

    @Override
    public String getBootstrapServers() {
        return kafkaProperties.getConsumer().getBootstrapServers();
    }

    @Override
    public String getKeyDeserializer() {
        return kafkaProperties.getConsumer().getKeyDeserializer();
    }

    @Override
    public String getValueDeserializer() {
        return kafkaProperties.getConsumer().getValueDeserializer();
    }

    @Override
    public int getMaxPollRecords() {
        return kafkaProperties.getConsumer().getMaxPollRecords();
    }

    @Override
    public int getFetchMaxBytes() {
        return kafkaProperties.getConsumer().getFetchMaxBytes();
    }

    @Override
    public int getMaxPartitionFetch() {
        return kafkaProperties.getConsumer().getMaxPartitionFetch();
    }
}
