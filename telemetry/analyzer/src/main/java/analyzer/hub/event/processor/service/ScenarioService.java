package analyzer.hub.event.processor.service;

import analyzer.hub.event.processor.entity.*;
import analyzer.hub.event.processor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ScenarioService {
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    public void addScenarios(List<HubEventAvro> events) {
        log.info("Processing {} events for addScenarios", events.size());

        Set<String> allSensorIds = new HashSet<>();
        Map<String, ScenarioData> scenarioDataMap = new LinkedHashMap<>();

        for (HubEventAvro event : events) {
            ScenarioAddedEventAvro payload = (ScenarioAddedEventAvro) event.getPayload();
            String hubId = event.getHubId();
            String scenarioName = payload.getName();

            List<String> sensorIds = new ArrayList<>();
            payload.getConditions().forEach(c -> sensorIds.add(c.getSensorId()));
            payload.getActions().forEach(a -> sensorIds.add(a.getSensorId()));

            allSensorIds.addAll(sensorIds);

            String key = hubId + ":" + scenarioName;
            scenarioDataMap.put(key, new ScenarioData(hubId, scenarioName, payload, sensorIds));
        }

        Set<String> existingSensorIds = sensorRepository.findExistingIds(allSensorIds);

        for (ScenarioData data : scenarioDataMap.values()) {
            for (String sensorId : data.sensorIds) {
                if (!existingSensorIds.contains(sensorId)) {
                    throw new RuntimeException(
                            String.format("Sensor not found: id=%s, hubId=%s", sensorId, data.hubId)
                    );
                }
            }
        }

        for (ScenarioData data : scenarioDataMap.values()) {
            saveScenario(data);
        }
    }

    public void removeScenarios(List<HubEventAvro> events) {
        log.info("Processing {} events for removeScenarios", events.size());

        Set<Long> scenarioIds = events.stream()
                .map(event -> {
                    ScenarioRemovedEventAvro payload = (ScenarioRemovedEventAvro) event.getPayload();
                    String hubId = event.getHubId();
                    String scenarioName = payload.getName();

                    Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, scenarioName)
                            .orElse(null);

                    if (scenario == null) {
                        log.warn("Scenario not found: hubId={}, name={}", hubId, scenarioName);
                        return null;
                    }

                    return scenario.getId();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (scenarioIds.isEmpty()) {
            log.warn("No existing scenarios found for deletion");
            return;
        }

        scenarioRepository.deleteAllById(scenarioIds);

        deleteOrphanConditions();
        deleteOrphanActions();
        log.info("removeScenarios completed. Removed {} scenarios", scenarioIds.size());
    }

    @Transactional(readOnly = true)
    public List<Scenario> getScenariosWithConditions(String hubId) {
        List<ScenarioConditionView> views = scenarioRepository.findScenarioConditions(hubId);

        Map<Long, Scenario> scenarioMap = new LinkedHashMap<>();

        for (ScenarioConditionView view : views) {
            Scenario scenario = scenarioMap.get(view.getScenarioId());
            if (scenario == null) {
                scenario = new Scenario();
                scenario.setId(view.getScenarioId());
                scenario.setHubId(view.getHubId());
                scenario.setName(view.getScenarioName());
                scenario.setConditions(new HashSet<>());
                scenarioMap.put(view.getScenarioId(), scenario);
            }

            if (view.getSensorId() != null && view.getConditionId() != null) {
                Sensor sensor = new Sensor();
                sensor.setId(view.getSensorId());

                Condition condition = new Condition();
                condition.setId(view.getConditionId());
                condition.setType(ConditionTypeAvro.valueOf(view.getConditionType()));
                condition.setOperation(ConditionOperationAvro.valueOf(view.getConditionOperation()));
                condition.setValue(view.getConditionValue());

                ScenarioCondition sc = new ScenarioCondition();
                sc.setId(new ScenarioConditionId(view.getScenarioId(), view.getSensorId(), view.getConditionId()));
                sc.setScenario(scenario);
                sc.setSensor(sensor);
                sc.setCondition(condition);

                scenario.getConditions().add(sc);
            }
        }

        return new ArrayList<>(scenarioMap.values());
    }

    @Transactional(readOnly = true)
    public List<Scenario> getScenariosWithActions(Set<Long> scenarioIds) {
        List<ScenarioActionView> views = scenarioRepository.findScenarioActions(scenarioIds);

        Map<Long, Scenario> scenarioMap = new LinkedHashMap<>();

        for (ScenarioActionView view : views) {
            Scenario scenario = scenarioMap.get(view.getScenarioId());
            if (scenario == null) {
                scenario = new Scenario();
                scenario.setId(view.getScenarioId());
                scenario.setName(view.getScenarioName());
                scenario.setActions(new HashSet<>());
                scenarioMap.put(view.getScenarioId(), scenario);
            }

            if (view.getSensorId() != null && view.getActionId() != null) {
                Sensor sensor = new Sensor();
                sensor.setId(view.getSensorId());

                Action action = new Action();
                action.setId(view.getActionId());
                action.setType(ActionTypeAvro.valueOf(view.getActionType()));
                action.setValue(view.getActionValue());

                ScenarioAction sa = new ScenarioAction();
                sa.setId(new ScenarioActionId(view.getScenarioId(), view.getSensorId(), view.getActionId()));
                sa.setScenario(scenario);
                sa.setSensor(sensor);
                sa.setAction(action);

                scenario.getActions().add(sa);
            }
        }

        return new ArrayList<>(scenarioMap.values());
    }

    private void saveScenario(ScenarioData data) {
        String hubId = data.hubId;
        String scenarioName = data.name;
        ScenarioAddedEventAvro payload = data.payload;

        List<Condition> conditions = getOrCreateConditions(payload.getConditions());
        List<Action> actions = getOrCreateActions(payload.getActions());

        Scenario scenario = scenarioRepository
                .findByHubIdAndName(hubId, scenarioName)
                .orElseGet(() -> createScenario(hubId, scenarioName));


        Set<String> allSensorIds = new HashSet<>();
        payload.getConditions().forEach(c -> allSensorIds.add(c.getSensorId()));
        payload.getActions().forEach(a -> allSensorIds.add(a.getSensorId()));

        Map<String, Sensor> sensorMap = validateAndLoadSensors(hubId, allSensorIds);

        updateScenarioConditions(scenario, conditions, payload.getConditions(), sensorMap);
        updateScenarioActions(scenario, actions, payload.getActions(), sensorMap);

        log.info("Successfully processed scenario: hubId={}, name={}, id={}",
                hubId, scenarioName, scenario.getId());
    }

    // ======== ПРИВАТНЫЕ МЕТОДЫ ========

    /**
     * Загружает все сенсоры из БД одним запросом и возвращает Map для быстрого доступа.
     * Проверяет, что все запрошенные сенсоры существуют в БД.
     *
     * @param hubId идентификатор хаба
     * @param sensorIds множество ID сенсоров для загрузки
     * @return Map<String, Sensor> где ключ - ID сенсора, значение - объект Sensor
     * @throws RuntimeException если какой-то сенсор не найден
     */
    private Map<String, Sensor> validateAndLoadSensors(String hubId,
                                                       Set<String> sensorIds) {
        List<Sensor> sensors = sensorRepository.findAllByIdInAndHubId(sensorIds, hubId);

        Map<String, Sensor> sensorMap = sensors.stream()
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));

        for (String sensorId : sensorIds) {
            if (!sensorMap.containsKey(sensorId)) {
                throw new RuntimeException(
                        String.format("Sensor not found: id=%s, hubId=%s", sensorId, hubId)
                );
            }
        }

        return sensorMap;
    }

    /**
     * Удаляет условия, которые не используются ни в одном сценарии.
     * Вызывается после удаления сценариев для очистки "мусора".
     */
    private void deleteOrphanConditions() {
        List<Long> orphanIds = scenarioConditionRepository.findOrphanConditionIds();
        if (!orphanIds.isEmpty()) {
            conditionRepository.deleteAllById(orphanIds);
            log.info("Deleted {} orphan conditions", orphanIds.size());
        }
    }

    /**
     * Удаляет действия, которые не используются ни в одном сценарии.
     * Вызывается после удаления сценариев для очистки "мусора".
     */
    private void deleteOrphanActions() {
        List<Long> orphanIds = scenarioActionRepository.findOrphanActionIds();
        if (!orphanIds.isEmpty()) {
            actionRepository.deleteAllById(orphanIds);
            log.info("Deleted {} orphan actions", orphanIds.size());
        }
    }

    /**
     * Получает существующие условия из БД или создает новые.
     * Для каждого условия проверяет существование по комбинации (type, operation, value).
     *
     * @param conditionAvros список условий из события
     * @return список объектов Condition (существующих или вновь созданных)
     */
    private List<Condition> getOrCreateConditions(List<ScenarioConditionAvro> conditionAvros) {
        List<Condition> conditions = new ArrayList<>();

        for (ScenarioConditionAvro avro : conditionAvros) {
            ConditionTypeAvro type = avro.getType();
            ConditionOperationAvro operation = avro.getOperation();
            Integer value = extractIntValue(avro.getValue());

            Condition condition = conditionRepository
                    .findByTypeAndOperationAndValue(type, operation, value)
                    .orElseGet(() -> {
                        Condition newCondition = new Condition();
                        newCondition.setType(type);
                        newCondition.setOperation(operation);
                        newCondition.setValue(value);
                        return conditionRepository.save(newCondition);
                    });

            conditions.add(condition);
        }

        return conditions;
    }

    /**
     * Получает существующие действия из БД или создает новые.
     * Для каждого действия проверяет существование по комбинации (type, value).
     *
     * @param actionAvros список действий из события
     * @return список объектов Action (существующих или вновь созданных)
     */
    private List<Action> getOrCreateActions(List<DeviceActionAvro> actionAvros) {
        List<Action> actions = new ArrayList<>();

        for (DeviceActionAvro avro : actionAvros) {
            ActionTypeAvro type = avro.getType();
            Integer value = extractIntValue(avro.getValue());

            Action action = actionRepository
                    .findByTypeAndValue(type, value)
                    .orElseGet(() -> {
                        Action newAction = new Action();
                        newAction.setType(type);
                        newAction.setValue(value);
                        return actionRepository.save(newAction);
                    });

            actions.add(action);
        }

        return actions;
    }

    /**
     * Создает новый сценарий в БД.
     *
     * @param hubId идентификатор хаба
     * @param name название сценария
     * @return созданный объект Scenario с присвоенным ID
     */
    private Scenario createScenario(String hubId, String name) {
        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(name);
        return scenarioRepository.save(scenario);
    }

    /**
     * Обновляет связи сценария с условиями.
     * Сначала удаляет все старые связи, затем создает новые.
     * Использует батчевый INSERT для оптимизации.
     *
     * @param scenario сценарий для обновления
     * @param conditions список условий
     * @param conditionAvros исходные данные условий (нужны для получения sensorId)
     * @param sensorMap Map с сенсорами (предварительно загруженными)
     */
    private void updateScenarioConditions(Scenario scenario,
                                          List<Condition> conditions,
                                          List<ScenarioConditionAvro> conditionAvros,
                                          Map<String, Sensor> sensorMap) {

        scenarioConditionRepository.deleteAllByScenarioId(scenario.getId());

        List<ScenarioCondition> scenarioConditions = IntStream.range(0, conditions.size())
                .mapToObj(i -> {
                    Condition condition = conditions.get(i);
                    ScenarioConditionAvro avro = conditionAvros.get(i);

                    Sensor sensor = sensorMap.get(avro.getSensorId());

                    ScenarioConditionId id = new ScenarioConditionId(
                            scenario.getId(),
                            sensor.getId(),
                            condition.getId()
                    );

                    ScenarioCondition sc = new ScenarioCondition();
                    sc.setId(id);
                    sc.setScenario(scenario);
                    sc.setSensor(sensor);
                    sc.setCondition(condition);
                    return sc;
                })
                .collect(Collectors.toList());

        if (!scenarioConditions.isEmpty()) {
            scenarioConditionRepository.saveAll(scenarioConditions);
        }

        log.debug("Updated {} conditions for scenario id={}", conditions.size(), scenario.getId());
    }

    /**
     * Обновляет связи сценария с действиями.
     * Сначала удаляет все старые связи, затем создает новые.
     * Использует батчевый INSERT для оптимизации.
     *
     * @param scenario сценарий для обновления
     * @param actions список действий
     * @param actionAvros исходные данные действий (нужны для получения sensorId)
     * @param sensorMap Map с сенсорами (предварительно загруженными)
     */
    private void updateScenarioActions(Scenario scenario,
                                       List<Action> actions,
                                       List<DeviceActionAvro> actionAvros,
                                       Map<String, Sensor> sensorMap) {

        scenarioActionRepository.deleteAllByScenarioId(scenario.getId());

        List<ScenarioAction> scenarioActions = IntStream.range(0, actions.size())
                .mapToObj(i -> {
                    Action action = actions.get(i);
                    DeviceActionAvro avro = actionAvros.get(i);

                    Sensor sensor = sensorMap.get(avro.getSensorId());

                    ScenarioActionId id = new ScenarioActionId(
                            scenario.getId(),
                            sensor.getId(),
                            action.getId()
                    );

                    ScenarioAction sa = new ScenarioAction();
                    sa.setId(id);
                    sa.setScenario(scenario);
                    sa.setSensor(sensor);
                    sa.setAction(action);
                    return sa;
                })
                .collect(Collectors.toList());

        if (!scenarioActions.isEmpty()) {
            scenarioActionRepository.saveAll(scenarioActions);
        }

        log.debug("Updated {} actions for scenario id={}", actions.size(), scenario.getId());
    }

    /**
     * @param value значение из Avro-схемы
     * @return Integer (как есть), Boolean (true→1, false→0), null.
     */
    private Integer extractIntValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return null;
    }

    private static class ScenarioData {
        String hubId;
        String name;
        ScenarioAddedEventAvro payload;
        List<String> sensorIds;

        ScenarioData(String hubId, String name, ScenarioAddedEventAvro payload, List<String> sensorIds) {
            this.hubId = hubId;
            this.name = name;
            this.payload = payload;
            this.sensorIds = sensorIds;
        }
    }
}
