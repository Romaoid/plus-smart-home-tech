package telcol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    private Topics topics = new Topics();
    private Producer producer = new Producer();

    @Data
    public static class Topics {
        private String sensors = "telemetry.sensors.v1";
        private String hubs = "telemetry.hubs.v1";
    }

    @Data
    public static class Producer {
        private String bootstrapServers = "localhost:9092";
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String valueSerializer = "tools.serializer.GeneralAvroSerializer";
    }
}
