package tools.sender;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import tools.producer.ProducerClient;

public abstract class BaseSender<T extends SpecificRecordBase> {

    private final ProducerClient client;
    private final String topic;

    protected BaseSender(ProducerClient client, String topic) {
        this.client = client;
        this.topic = topic;
    }

    public void sendEvent(T message) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, message);
        client.getProducer().send(record);
    }

    public void stop() {
        client.stop();
    }
}
