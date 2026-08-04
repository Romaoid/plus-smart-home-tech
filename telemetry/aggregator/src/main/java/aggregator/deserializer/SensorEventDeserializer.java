package aggregator.deserializer;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import tools.deserializer.BaseAvroDeserializer;

public class SensorEventDeserializer extends BaseAvroDeserializer<SensorEventAvro> {
    public SensorEventDeserializer() {
        super(SensorEventAvro.getClassSchema());
    }
}
