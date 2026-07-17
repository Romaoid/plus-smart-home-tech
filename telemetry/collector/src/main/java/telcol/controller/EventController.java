package telcol.controller;

import telcol.dto.hub.device.added.DeviceAddedEvent;
import telcol.dto.hub.device.removed.DeviceRemovedEvent;
import telcol.dto.hub.scenario.added.ScenarioAddedEvent;
import telcol.dto.hub.scenario.removed.ScenarioRemovedEvent;
import telcol.dto.sensor.*;
import jakarta.validation.Valid;
import telcol.dto.hub.HubEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import telcol.mapper.HubMapper;
import telcol.mapper.SensorMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import telcol.sender.HubV1Sender;
import telcol.sender.SensorV1Sender;

@RestController
@RequestMapping("/events")
@Slf4j
@AllArgsConstructor
public class EventController {
    private HubV1Sender hubV1Sender;
    private SensorV1Sender sensorV1Sender;

    @PostMapping("/sensors")
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        try {
            SensorEventAvro avroEvent = switch (event) {
                case ClimateSensorEvent e -> SensorMapper.mapToAvro(e);
                case LightSensorEvent e -> SensorMapper.mapToAvro(e);
                case MotionSensorEvent e -> SensorMapper.mapToAvro(e);
                case SwitchSensorEvent e -> SensorMapper.mapToAvro(e);
                case TemperatureSensorEvent e -> SensorMapper.mapToAvro(e);
                default -> throw new IllegalArgumentException(
                        "Unsupported event type: " + event.getClass().getSimpleName()
                );
            };

            sensorV1Sender.sendEvent(avroEvent);
        } catch (IllegalArgumentException e) {
            log.error("Failed to process event: {}", e.getMessage());
        }
    }

    @PostMapping("/hubs")
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        try {
            HubEventAvro avroEvent = switch (event) {
                case DeviceAddedEvent e -> HubMapper.mapToAvro(e);
                case DeviceRemovedEvent e -> HubMapper.mapToAvro(e);
                case ScenarioAddedEvent e -> HubMapper.mapToAvro(e);
                case ScenarioRemovedEvent e -> HubMapper.mapToAvro(e);
                default -> throw new IllegalArgumentException(
                        "Unsupported event type: " + event.getClass().getSimpleName()
                );
            };

            hubV1Sender.sendEvent(avroEvent);
        } catch (IllegalArgumentException e) {
            log.error("Failed to process event: {}", e.getMessage());
        }
    }
}
