package analyzer.hub.event.processor.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaPropertiesHub {
    private Topics topics = new Topics();
    private Consumer hubsConsumer = new Consumer();
    @Getter
    private int consumerAttemptTimeout = 1000;

    @Data
    public static class Topics {
        private List<String> hubs = List.of("telemetry.hubs.v1");
    }

    @Data
    public static class Consumer {
        private String bootstrapServers = "localhost:9092";
        private String clientId = "AnalyzerHubsConsumer";
        private String groupId = "Analyzer.HubEvents.group.id";
        private String keyDeserializer = "org.apache.kafka.common.serialization.VoidDeserializer";
        private String valueDeserializer = "analyzer.hub.event.processor.deserializer.HubEventDeserializer";
        private Integer maxPollRecords = 200;
        private Integer fetchMaxBytes = 5242880;
        private Integer maxPartitionFetch = 524288;
    }
}
