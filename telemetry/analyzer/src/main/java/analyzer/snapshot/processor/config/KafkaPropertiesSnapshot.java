package analyzer.snapshot.processor.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaPropertiesSnapshot {
    private Topics topics = new Topics();
    private Consumer snapshotsConsumer = new Consumer();
    @Getter
    private int consumerAttemptTimeout = 1000;

    @Data
    public static class Topics {
        private List<String> snapshots = List.of("telemetry.snapshots.v1");
    }

    @Data
    public static class Consumer {
        private String bootstrapServers = "localhost:9092";
        private String clientId = "AnalyzerSnapshotsConsumer";
        private String groupId = "Analyzer.Snapshots.group.id";
        private String keyDeserializer = "org.apache.kafka.common.serialization.VoidDeserializer";
        private String valueDeserializer = "analyzer.snapshot.processor.deserializer.SnapshotDeserializer";
        private Integer maxPollRecords = 200;
        private Integer fetchMaxBytes = 6144000;
        private Integer maxPartitionFetch = 614400;
        private Boolean enableAutoCommit = false;
    }
}
