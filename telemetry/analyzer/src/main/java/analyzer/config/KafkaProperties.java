package analyzer.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    private Topics topics = new Topics();
    private HubsConsumer hubsConsumer = new HubsConsumer();
    private SnapshotsConsumer snapshotsConsumer = new SnapshotsConsumer();
    @Getter
    private int consumerAttemptTimeout;

    @Data
    public static class Topics {
        private String hubs;
        private String snapshots;

        public List<String> getSnapshotsList() {
            return Arrays.asList(snapshots.split(","));
        }

        public List<String> getHubsList() {
            return Arrays.asList(hubs.split(","));
        }
    }

    @Data
    public static class HubsConsumer {
        private String bootstrapServers;
        private String clientId;
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
        private Integer maxPollRecords;
        private Integer fetchMaxBytes;
        private Integer maxPartitionFetch;
    }

    @Data
    public static class SnapshotsConsumer {
        private String bootstrapServers;
        private String clientId;
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
        private Integer maxPollRecords;
        private Integer fetchMaxBytes;
        private Integer maxPartitionFetch;
        private Boolean enableAutoCommit;
    }
}
