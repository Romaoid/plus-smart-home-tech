package analyzer.snapshot.processor.deserializer;

import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import tools.deserializer.BaseAvroDeserializer;

public class SnapshotDeserializer extends BaseAvroDeserializer<SensorsSnapshotAvro> {
    public SnapshotDeserializer() {
        super(SensorsSnapshotAvro.getClassSchema());
    }
}
