package tools.producer;

public interface ProducerConfigProvider {
    String getBootstrapServers();
    String getKeySerializer();
    String getValueSerializer();
}
