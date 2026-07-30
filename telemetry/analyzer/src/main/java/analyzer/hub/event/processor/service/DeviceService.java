package analyzer.hub.event.processor.service;

import analyzer.hub.event.processor.entity.Sensor;
import analyzer.hub.event.processor.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DeviceService {
    private final SensorRepository sensorRepository;

    public void addDevices(List<HubEventAvro> events) {

        Map<String, Set<String>> hubDevicesMap = events.stream()
                .collect(Collectors.groupingBy(
                        HubEventAvro::getHubId,
                        Collectors.mapping(
                                event -> ((DeviceAddedEventAvro) event.getPayload()).getId(),
                                Collectors.toSet()
                        )
                ));

        Map<String, Set<String>> existingDevicesMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : hubDevicesMap.entrySet()) {
            String hubId = entry.getKey();
            Set<String> deviceIds = entry.getValue();

            Set<String> existingIds = sensorRepository.findExistingIdsByHubId(deviceIds, hubId);
            existingDevicesMap.put(hubId, existingIds);
        }

        List<Sensor> sensorsToSave = new ArrayList<>();
        int duplicateCount = 0;

        for (HubEventAvro event : events) {
            String hubId = event.getHubId();
            String deviceId = ((DeviceAddedEventAvro) event.getPayload()).getId();

            Set<String> existingIds = existingDevicesMap.get(hubId);
            if (existingIds != null && existingIds.contains(deviceId)) {
                duplicateCount++;
                continue;
            }

            Sensor sensor = new Sensor();
            sensor.setId(deviceId);
            sensor.setHubId(hubId);
            sensorsToSave.add(sensor);
        }

        if (!sensorsToSave.isEmpty()) {
            sensorRepository.saveAll(sensorsToSave);
            log.info("Added {} new devices", sensorsToSave.size());
        }

        if (duplicateCount > 0) {
            log.warn("Skipped {} duplicate devices", duplicateCount);
        }
    }

    public void removeDevices(List<HubEventAvro> events) {
        log.info("Processing {} events for removeDevices", events.size());

        Map<String, List<String>> hubDevicesMap = events.stream()
                .collect(Collectors.groupingBy(
                        HubEventAvro::getHubId,
                        Collectors.mapping(
                                event -> ((DeviceRemovedEventAvro) event.getPayload()).getId(),
                                Collectors.toList()
                        )
                ));

        List<Sensor> sensorsToDelete = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : hubDevicesMap.entrySet()) {
            String hubId = entry.getKey();
            List<String> deviceIds = entry.getValue();

            List<Sensor> existingSensors = sensorRepository.findAllById(deviceIds);

            List<Sensor> hubSensors = existingSensors.stream()
                    .filter(sensor -> sensor.getHubId().equals(hubId))
                    .toList();

            if (hubSensors.isEmpty()) {
                log.warn("No existing devices found for hubId={}", hubId);
                continue;
            }

            sensorsToDelete.addAll(hubSensors);
            log.info("Found {} devices to delete from hubId={}", hubSensors.size(), hubId);
        }

        if (sensorsToDelete.isEmpty()) {
            log.warn("No existing devices found for deletion");
            return;
        }

        sensorRepository.deleteAll(sensorsToDelete);
        log.info("Deleted {} devices", sensorsToDelete.size());
    }
}
