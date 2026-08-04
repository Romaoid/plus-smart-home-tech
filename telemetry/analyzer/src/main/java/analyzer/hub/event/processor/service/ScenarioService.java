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

        Map<String, ScenarioData> scenarioDataMap = new LinkedHashMap<>();

        for (HubEventAvro event : events) {
            ScenarioAddedEventAvro payload = (ScenarioAddedEventAvro) event.getPayload();
            String hubId = event.getHubId();
            String scenarioName = payload.getName();

            List<String> sensorIds = new ArrayList<>();
            payload.getConditions().forEach(c -> sensorIds.add(c.getSensorId()));
            payload.getActions().forEach(a -> sensorIds.add(a.getSensorId()));


            String key = hubId + ":" + scenarioName;
            scenarioDataMap.put(key, new ScenarioData(hubId, scenarioName, payload, sensorIds));
        }

        saveScenarios(scenarioDataMap);
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

    // ======== ПРИВАТНЫЕ МЕТОДЫ ========

    private void saveScenarios(Map<String, ScenarioData> scenarioDataMap) {
        log.info("Starting batch save for {} scenarios", scenarioDataMap.size());

        Set<String> keys = scenarioDataMap.keySet();
        List<Scenario> existingScenariosList = scenarioRepository.findByHubIdAndNameIn(keys);

        Map<String, Scenario> existingScenarios = existingScenariosList.stream()
                .collect(Collectors.toMap(
                        s -> s.getHubId() + ":" + s.getName(),
                        Function.identity()
                ));

        List<Scenario> scenariosToCreate = new ArrayList<>();

        List<Long> existingScenarioIds = new ArrayList<>();

        for (ScenarioData data : scenarioDataMap.values()) {
            String key = data.hubId + ":" + data.name;
            Scenario scenario = existingScenarios.get(key);

            if (scenario == null) {
                scenario = new Scenario();
                scenario.setHubId(data.hubId);
                scenario.setName(data.name);
                scenariosToCreate.add(scenario);

                existingScenarios.put(key, scenario);
            } else {
                existingScenarioIds.add(scenario.getId());
            }
        }

        if (!scenariosToCreate.isEmpty()) {
            List<Scenario> savedScenarios = scenarioRepository.saveAll(scenariosToCreate);

            for (Scenario scenario : savedScenarios) {
                String key = scenario.getHubId() + ":" + scenario.getName();
                existingScenarios.put(key, scenario);
                existingScenarioIds.add(scenario.getId());
            }
            log.info("Created {} new scenarios", savedScenarios.size());
        }

        List<ScenarioConditionAvro> allConditionAvros = new ArrayList<>();
        List<DeviceActionAvro> allActionAvros = new ArrayList<>();

        Map<String, List<ScenarioConditionAvro>> conditionsByScenario = new HashMap<>();
        Map<String, List<DeviceActionAvro>> actionsByScenario = new HashMap<>();

        for (Map.Entry<String, ScenarioData> entry : scenarioDataMap.entrySet()) {
            String key = entry.getKey();
            ScenarioData data = entry.getValue();

            conditionsByScenario.put(key, data.payload.getConditions());
            actionsByScenario.put(key, data.payload.getActions());
            allConditionAvros.addAll(data.payload.getConditions());
            allActionAvros.addAll(data.payload.getActions());
        }

        Map<String, Condition> conditionMap = getOrCreateConditionsBatch(allConditionAvros);

        Map<String, Action> actionMap = getOrCreateActionsBatch(allActionAvros);

        Set<String> allSensorIds = scenarioDataMap.values().stream()
                .flatMap(data -> data.sensorIds.stream())
                .collect(Collectors.toSet());

        Map<String, Sensor> sensorMap = loadSensorsBatch(allSensorIds);

        if (!existingScenarioIds.isEmpty()) {
            scenarioConditionRepository.deleteAllByScenarioIdIn(existingScenarioIds);
            scenarioActionRepository.deleteAllByScenarioIdIn(existingScenarioIds);
            log.debug("Deleted old relations for {} scenarios", existingScenarioIds.size());
        }

        List<ScenarioCondition> allScenarioConditions = new ArrayList<>();
        List<ScenarioAction> allScenarioActions = new ArrayList<>();

        for (Map.Entry<String, ScenarioData> entry : scenarioDataMap.entrySet()) {
            String key = entry.getKey();
            ScenarioData data = entry.getValue();
            Scenario scenario = existingScenarios.get(key);

            List<ScenarioCondition> scenarioConditions = buildScenarioConditions(
                    scenario,
                    conditionsByScenario.get(key),
                    conditionMap,
                    sensorMap
            );
            allScenarioConditions.addAll(scenarioConditions);

            List<ScenarioAction> scenarioActions = buildScenarioActions(
                    scenario,
                    actionsByScenario.get(key),
                    actionMap,
                    sensorMap
            );
            allScenarioActions.addAll(scenarioActions);
        }

        if (!allScenarioConditions.isEmpty()) {
            scenarioConditionRepository.saveAll(allScenarioConditions);
            log.debug("Saved {} scenario-condition relations", allScenarioConditions.size());
        }

        if (!allScenarioActions.isEmpty()) {
            scenarioActionRepository.saveAll(allScenarioActions);
            log.debug("Saved {} scenario-action relations", allScenarioActions.size());
        }

        log.info("Batch save completed successfully for {} scenarios", scenarioDataMap.size());
    }

    private Map<String, Condition> getOrCreateConditionsBatch(List<ScenarioConditionAvro> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return new HashMap<>();
        }

        Set<String> conditionKeysSet = conditions.stream()
                .map(avro -> avro.getType() + ":" + avro.getOperation() + ":" + extractIntValue(avro.getValue()))
                .collect(Collectors.toSet());

        List<Condition> existingConditionsList = conditionRepository.findByTypeOperationValueIn(conditionKeysSet);

        Map<String, Condition> existingConditions = existingConditionsList.stream()
                .collect(Collectors.toMap(
                        c -> c.getType() + ":" + c.getOperation() + ":" + c.getValue(),
                        Function.identity()
                ));

        List<Condition> conditionsToCreate = new ArrayList<>();
        Map<String, Condition> resultMap = new HashMap<>(existingConditions);

        for (ScenarioConditionAvro avro : conditions) {
            String key = avro.getType() + ":" + avro.getOperation() + ":" + extractIntValue(avro.getValue());
            if (!resultMap.containsKey(key)) {
                Condition newCondition = new Condition();
                newCondition.setType(avro.getType());
                newCondition.setOperation(avro.getOperation());
                newCondition.setValue(extractIntValue(avro.getValue()));
                conditionsToCreate.add(newCondition);
                resultMap.put(key, newCondition); // Временно без ID
            }
        }

        if (!conditionsToCreate.isEmpty()) {
            List<Condition> savedConditions = conditionRepository.saveAll(conditionsToCreate);

            for (Condition condition : savedConditions) {
                String key = condition.getType() + ":" + condition.getOperation() + ":" + condition.getValue();
                resultMap.put(key, condition);
            }
            log.info("Created {} new conditions", savedConditions.size());
        }

        return resultMap;
    }

    private Map<String, Action> getOrCreateActionsBatch(List<DeviceActionAvro> actions) {
        if (actions == null || actions.isEmpty()) {
            return new HashMap<>();
        }

        Set<String> actionKeysSet = actions.stream()
                .map(avro -> avro.getType() + ":" + extractIntValue(avro.getValue()))
                .collect(Collectors.toSet());

        List<Action> existingActionsList = actionRepository.findByTypeValueIn(actionKeysSet);

        Map<String, Action> existingActions = existingActionsList.stream()
                .collect(Collectors.toMap(
                        a -> a.getType() + ":" + a.getValue(),
                        Function.identity()
                ));

        List<Action> actionsToCreate = new ArrayList<>();
        Map<String, Action> resultMap = new HashMap<>(existingActions);

        for (DeviceActionAvro avro : actions) {
            String key = avro.getType() + ":" + extractIntValue(avro.getValue());
            if (!resultMap.containsKey(key)) {
                Action newAction = new Action();
                newAction.setType(avro.getType());
                newAction.setValue(extractIntValue(avro.getValue()));
                actionsToCreate.add(newAction);
                resultMap.put(key, newAction);
            }
        }

        if (!actionsToCreate.isEmpty()) {
            List<Action> savedActions = actionRepository.saveAll(actionsToCreate);

            for (Action action : savedActions) {
                String key = action.getType() + ":" + action.getValue();
                resultMap.put(key, action);
            }
            log.info("Created {} new actions", savedActions.size());
        }

        return resultMap;
    }

    private Map<String, Sensor> loadSensorsBatch(Set<String> sensorIds) {
        if (sensorIds == null || sensorIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Sensor> sensors = sensorRepository.findAllByIdIn(sensorIds);

        Map<String, Sensor> sensorMap = sensors.stream()
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));

        log.debug("Loaded {} sensors", sensorMap.size());
        return sensorMap;
    }

    private List<ScenarioCondition> buildScenarioConditions(
            Scenario scenario,
            List<ScenarioConditionAvro> conditionAvros,
            Map<String, Condition> conditionMap,
            Map<String, Sensor> sensorMap) {

        List<ScenarioCondition> scenarioConditions = new ArrayList<>();

        for (ScenarioConditionAvro avro : conditionAvros) {
            String conditionKey = avro.getType() + ":" + avro.getOperation() + ":" + extractIntValue(avro.getValue());
            String sensorId = avro.getSensorId();

            Condition condition = conditionMap.get(conditionKey);
            Sensor sensor = sensorMap.get(sensorId);

            if (condition == null) {
                throw new RuntimeException("Condition not found: " + conditionKey);
            }
            if (sensor == null) {
                throw new RuntimeException("Sensor not found: " + sensorId);
            }

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
            scenarioConditions.add(sc);
        }

        return scenarioConditions;
    }

    private List<ScenarioAction> buildScenarioActions(
            Scenario scenario,
            List<DeviceActionAvro> actionAvros,
            Map<String, Action> actionMap,
            Map<String, Sensor> sensorMap) {

        List<ScenarioAction> scenarioActions = new ArrayList<>();

        for (DeviceActionAvro avro : actionAvros) {
            String actionKey = avro.getType() + ":" + extractIntValue(avro.getValue());
            String sensorId = avro.getSensorId();

            Action action = actionMap.get(actionKey);
            Sensor sensor = sensorMap.get(sensorId);

            if (action == null) {
                throw new RuntimeException("Action not found: " + actionKey);
            }
            if (sensor == null) {
                throw new RuntimeException("Sensor not found: " + sensorId);
            }

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
            scenarioActions.add(sa);
        }

        return scenarioActions;
    }

    private void deleteOrphanConditions() {
        List<Long> orphanIds = scenarioConditionRepository.findOrphanConditionIds();
        if (!orphanIds.isEmpty()) {
            conditionRepository.deleteAllById(orphanIds);
            log.info("Deleted {} orphan conditions", orphanIds.size());
        }
    }

    private void deleteOrphanActions() {
        List<Long> orphanIds = scenarioActionRepository.findOrphanActionIds();
        if (!orphanIds.isEmpty()) {
            actionRepository.deleteAllById(orphanIds);
            log.info("Deleted {} orphan actions", orphanIds.size());
        }
    }

    private Integer extractIntValue(Object value) {
        return switch (value) {
            case null -> null;
            case Integer i -> i;
            case Boolean b -> b ? 1 : 0;
            default -> null;
        };
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
