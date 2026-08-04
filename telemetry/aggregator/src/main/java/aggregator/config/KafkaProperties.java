package aggregator.config;

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
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    @Getter
    private int consumerAttemptTimeout = 1000;

    @Data
    public static class Topics {
        private String sensors;
        private String snapshots;

        public List<String> getSensorsList() {
            return Arrays.asList(sensors.split(","));
        }
    }

    @Data
    public static class Producer {
        private String bootstrapServers;
        private String keySerializer;
        private String valueSerializer;
    }

    @Data
    public static class Consumer {
        private String clientId;
        private String groupId;
        private String bootstrapServers;
        private String keyDeserializer;
        private String valueDeserializer;

        private int maxPollRecords;
        private int fetchMaxBytes;
        private int maxPartitionFetch;
    }
}