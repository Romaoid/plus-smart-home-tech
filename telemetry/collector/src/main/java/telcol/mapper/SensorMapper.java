package telcol.mapper;

import telcol.dto.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

public class SensorMapper {
    public static SensorEventAvro mapToAvro(ClimateSensorEvent e) {
        ClimateSensorAvro sensor = ClimateSensorAvro.newBuilder()
                .setCo2Level(e.getCo2Level())
                .setHumidity(e.getHumidity())
                .setTemperatureC(e.getTemperatureC())
                .build();

        return buildSensorEvent(e, sensor);
    }

    public static SensorEventAvro mapToAvro(LightSensorEvent e) {
        LightSensorAvro sensor = LightSensorAvro.newBuilder()
                .setLinkQuality(e.getLinkQuality())
                .setLuminosity(e.getLuminosity())
                .build();

        return buildSensorEvent(e, sensor);
    }

    public static SensorEventAvro mapToAvro(MotionSensorEvent e) {
        MotionSensorAvro sensor = MotionSensorAvro.newBuilder()
                .setLinkQuality(e.getLinkQuality())
                .setMotion(e.isMotion())
                .setVoltage(e.getVoltage())
                .build();

        return buildSensorEvent(e, sensor);
    }

    public static SensorEventAvro mapToAvro(SwitchSensorEvent e) {
        SwitchSensorAvro sensor = SwitchSensorAvro.newBuilder()
                .setState(e.isState())
                .build();

        return buildSensorEvent(e, sensor);
    }

    public static SensorEventAvro mapToAvro(TemperatureSensorEvent e) {
        TemperatureSensorAvro sensor = TemperatureSensorAvro.newBuilder()
                .setTemperatureC(e.getTemperatureC())
                .setTemperatureF(e.getTemperatureF())
                .build();

        return buildSensorEvent(e, sensor);
    }

    private static SensorEventAvro buildSensorEvent(SensorEvent event, Object payload) {
        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }
}