package telcol.sender;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import telcol.producer.ProducerClient;

@Component
public abstract class BaseEventSender<T extends SpecificRecordBase> {
    private final ProducerClient client;
    private final String topic;

    protected BaseEventSender(ProducerClient client, String topic) {
        this.client = client;
        this.topic = topic;
    }

    public void sendEvent(T message) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, message);
        client.getProducer().send(record);
    }
}
