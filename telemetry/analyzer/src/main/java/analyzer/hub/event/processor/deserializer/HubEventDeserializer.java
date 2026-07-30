package analyzer.hub.event.processor.deserializer;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import tools.deserializer.BaseAvroDeserializer;

public class HubEventDeserializer extends BaseAvroDeserializer<HubEventAvro> {
    public HubEventDeserializer() {
        super(HubEventAvro.getClassSchema());
    }
}
