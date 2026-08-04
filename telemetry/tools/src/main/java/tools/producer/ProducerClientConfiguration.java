package tools.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Slf4j
@Configuration
public class ProducerClientConfiguration {

    @Bean
    ProducerClient getClient(ProducerConfigProvider config) {
        return new ProducerClient() {
            private Producer<String, SpecificRecordBase> producer;

            @Override
            public Producer<String, SpecificRecordBase> getProducer() {
                if (producer == null) {
                    initProducer(config);
                }
                return producer;
            }

            private void initProducer(ProducerConfigProvider configProvider) {
                Properties config = new Properties();

                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configProvider.getBootstrapServers());
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, configProvider.getKeySerializer());
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, configProvider.getValueSerializer());

                log.info("Initializing Kafka producer with bootstrap servers: {}",
                        configProvider.getBootstrapServers());
                log.debug("Kafka producer config: {}", config);

                producer = new KafkaProducer<>(config);
            }

            @Override
            public void stop() {
                if (producer != null) {
                    producer.flush();
                    producer.close();
                    log.info("Kafka producer closed");
                }
            }
        };
    }
}