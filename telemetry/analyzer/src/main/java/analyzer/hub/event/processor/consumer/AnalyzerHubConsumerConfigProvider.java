package analyzer.hub.event.processor.consumer;

import analyzer.hub.event.processor.config.KafkaPropertiesHub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.consumer.ConsumerConfigProvider;

@Component
@RequiredArgsConstructor
public class AnalyzerHubConsumerConfigProvider implements ConsumerConfigProvider {
    private final KafkaPropertiesHub kafkaProperties;
    @Override
    public String getClientId() {
        return kafkaProperties.getHubsConsumer().getClientId();
    }

    @Override
    public String getGroupId() {
        return kafkaProperties.getHubsConsumer().getGroupId();
    }

    @Override
    public String getBootstrapServers() {
        return kafkaProperties.getHubsConsumer().getBootstrapServers();
    }

    @Override
    public String getKeyDeserializer() {
        return kafkaProperties.getHubsConsumer().getKeyDeserializer();
    }

    @Override
    public String getValueDeserializer() {
        return kafkaProperties.getHubsConsumer().getValueDeserializer();
    }

    @Override
    public int getMaxPollRecords() {
        return kafkaProperties.getHubsConsumer().getMaxPollRecords();
    }

    @Override
    public int getFetchMaxBytes() {
        return kafkaProperties.getHubsConsumer().getFetchMaxBytes();
    }

    @Override
    public int getMaxPartitionFetch() {
        return kafkaProperties.getHubsConsumer().getMaxPartitionFetch();
    }
}
