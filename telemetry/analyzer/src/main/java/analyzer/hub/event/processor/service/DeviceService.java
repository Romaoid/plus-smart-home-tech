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

        Set<String> allDeviceIds = hubDevicesMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        List<Sensor> allExistingSensors = sensorRepository.findAllByIdIn(allDeviceIds);

        Set<String> existingKeys = allExistingSensors.stream()
                .map(sensor -> sensor.getHubId() + ":" + sensor.getId())
                .collect(Collectors.toSet());

        List<Sensor> sensorsToSave = new ArrayList<>();
        int duplicateCount = 0;

        for (HubEventAvro event : events) {
            String hubId = event.getHubId();
            String deviceId = ((DeviceAddedEventAvro) event.getPayload()).getId();
            String key = hubId + ":" + deviceId;

            if (existingKeys.contains(key)) {
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

        Set<String> allDeviceIds = hubDevicesMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        if (allDeviceIds.isEmpty()) {
            log.warn("No device IDs provided for deletion");
            return;
        }

        List<Sensor> allExistingSensors = sensorRepository.findAllByIdIn(allDeviceIds);

        if (allExistingSensors.isEmpty()) {
            log.warn("No existing devices found for deletion");
            return;
        }

        Set<String> deviceKeysToDelete = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : hubDevicesMap.entrySet()) {
            String hubId = entry.getKey();
            for (String deviceId : entry.getValue()) {
                deviceKeysToDelete.add(hubId + ":" + deviceId);
            }
        }

        List<Sensor> sensorsToDelete = allExistingSensors.stream()
                .filter(sensor -> deviceKeysToDelete.contains(sensor.getHubId() + ":" + sensor.getId()))
                .collect(Collectors.toList());

        if (sensorsToDelete.isEmpty()) {
            log.warn("No existing devices found for deletion");
            return;
        }

        sensorRepository.deleteAll(sensorsToDelete);
        log.info("Deleted {} devices", sensorsToDelete.size());
    }
}
