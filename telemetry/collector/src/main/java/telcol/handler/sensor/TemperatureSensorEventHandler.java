package telcol.handler.sensor;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;
import telcol.sender.SensorV1Sender;

@Component
@AllArgsConstructor
public class TemperatureSensorEventHandler implements SensorEventHandler {
    private SensorV1Sender sensorV1Sender;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.TEMPERATURE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        TemperatureSensorAvro sensor = TemperatureSensorAvro.newBuilder()
                .setTemperatureC(event.getTemperatureSensor().getTemperatureC())
                .setTemperatureF(event.getTemperatureSensor().getTemperatureF())
                .build();

        sensorV1Sender.sendEvent(
                build(event, sensor)
        );
    }
}
