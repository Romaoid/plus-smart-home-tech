package analyzer.snapshot.processor.consumer;

import analyzer.config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.consumer.ConsumerConfigProvider;

@Component
@RequiredArgsConstructor
public class AnalyzerSnapshotConsumerConfigProvider implements ConsumerConfigProvider {
    private final KafkaProperties kafkaProperties;

    @Override
    public String getClientId() {
        return kafkaProperties.getSnapshotsConsumer().getClientId();
    }

    @Override
    public String getGroupId() {
        return kafkaProperties.getSnapshotsConsumer().getGroupId();
    }

    @Override
    public String getBootstrapServers() {
        return kafkaProperties.getSnapshotsConsumer().getBootstrapServers();
    }

    @Override
    public String getKeyDeserializer() {
        return kafkaProperties.getSnapshotsConsumer().getKeyDeserializer();
    }

    @Override
    public String getValueDeserializer() {
        return kafkaProperties.getSnapshotsConsumer().getValueDeserializer();
    }

    @Override
    public int getMaxPollRecords() {
        return kafkaProperties.getSnapshotsConsumer().getMaxPollRecords();
    }

    @Override
    public int getFetchMaxBytes() {
        return kafkaProperties.getSnapshotsConsumer().getFetchMaxBytes();
    }

    @Override
    public int getMaxPartitionFetch() {
        return kafkaProperties.getSnapshotsConsumer().getMaxPartitionFetch();
    }

    @Override
    public boolean getEnableAutoCommit() {
        return kafkaProperties.getSnapshotsConsumer().getEnableAutoCommit();
    }
}
