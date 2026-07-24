package telcol.handler.sensor;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import telcol.sender.SensorV1Sender;

@Component
@AllArgsConstructor
public class SwitchSensorEventHandler implements SensorEventHandler {
    private SensorV1Sender sensorV1Sender;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        SwitchSensorAvro sensor = SwitchSensorAvro.newBuilder()
                .setState(event.getSwitchSensor().getState())
                .build();

        sensorV1Sender.sendEvent(
                build(event, sensor)
        );
    }
}