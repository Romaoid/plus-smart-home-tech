package telcol.handler.sensor;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import telcol.sender.SensorV1Sender;

@Component
@AllArgsConstructor
public class LightSensorEventHandler implements SensorEventHandler {
    private SensorV1Sender sensorV1Sender;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.LIGHT_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        LightSensorAvro sensor = LightSensorAvro.newBuilder()
                .setLinkQuality(event.getLightSensor().getLinkQuality())
                .setLuminosity(event.getLightSensor().getLuminosity())
                .build();

        sensorV1Sender.sendEvent(
                build(event, sensor)
        );
    }
}