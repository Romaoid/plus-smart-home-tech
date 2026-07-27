package telcol.handler.sensor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import telcol.sender.SensorV1Sender;

@Component
@AllArgsConstructor
@Slf4j
public class ClimateSensorEventHandler implements SensorEventHandler {
    private SensorV1Sender sensorV1Sender;

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.CLIMATE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        ClimateSensorAvro sensor = ClimateSensorAvro.newBuilder()
                .setCo2Level(event.getClimateSensor().getCo2Level())
                .setHumidity(event.getClimateSensor().getHumidity())
                .setTemperatureC(event.getClimateSensor().getTemperatureC())
                .build();

        log.debug("Отправляю: {}, {}",event, sensor);
        sensorV1Sender.sendEvent(
                build(event, sensor)
        );
    }
}
