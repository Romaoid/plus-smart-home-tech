package telcol.handler.sensor;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import telcol.sender.SensorV1Sender;

@Component
@AllArgsConstructor
public class MotionSensorEventHandler implements SensorEventHandler {
    private SensorV1Sender sensorV1Sender;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.MOTION_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        MotionSensorAvro sensor = MotionSensorAvro.newBuilder()
                .setLinkQuality(event.getMotionSensor().getLinkQuality())
                .setMotion(event.getMotionSensor().getMotion())
                .setVoltage(event.getMotionSensor().getVoltage())
                .build();

        sensorV1Sender.sendEvent(
                build(event, sensor)
        );
    }
}
