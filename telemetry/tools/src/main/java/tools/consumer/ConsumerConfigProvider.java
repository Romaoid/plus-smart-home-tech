package tools.consumer;

public interface ConsumerConfigProvider {
    String getClientId();
    String getGroupId();
    String getBootstrapServers();
    String getKeyDeserializer();
    String getValueDeserializer();
    int getMaxPollRecords();
    int getFetchMaxBytes();
    int getMaxPartitionFetch();
}
