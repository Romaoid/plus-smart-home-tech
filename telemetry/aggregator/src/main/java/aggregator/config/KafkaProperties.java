package aggregator.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    private Topics topics = new Topics();
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    @Getter
    private int consumerAttemptTimeout = 1000;

    @Data
    public static class Topics {
        private List<String> sensors = List.of("telemetry.sensors.v1");
        private String snapshots = "telemetry.snapshots.v1";
    }

    @Data
    public static class Producer {
        private String bootstrapServers = "localhost:9092";
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String valueSerializer = "tools.serializer.GeneralAvroSerializer";
    }

    @Data
    public static class Consumer {
        private String clientId = "AggregationConsumer";
        private String groupId = "Aggregation.group.id";
        private String bootstrapServers = "localhost:9092";
        private String keyDeserializer = org.apache.kafka.common.serialization.VoidDeserializer.class.getCanonicalName();
        private String valueDeserializer = aggregator.deserializer.SensorEventDeserializer.class.getName();

        private int maxPollRecords = 100;
        private int fetchMaxBytes = 3072000;
        private int maxPartitionFetch = 307200;
    }
}
