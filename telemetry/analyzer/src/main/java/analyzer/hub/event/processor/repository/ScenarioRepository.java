package analyzer.hub.event.processor.repository;

import analyzer.hub.event.processor.entity.Scenario;
import analyzer.hub.event.processor.entity.ScenarioActionView;
import analyzer.hub.event.processor.entity.ScenarioConditionView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    Optional<Scenario> findByHubIdAndName(String hubId, String name);

    @Query("SELECT s.id as scenarioId, " +
            "s.hubId as hubId, " +
            "s.name as scenarioName, " +
            "sc.sensor.id as sensorId, " +
           "c.id as conditionId, " +
            "c.type as conditionType, " +
            "c.operation as conditionOperation, " +
            "c.value as conditionValue " +
        "FROM Scenario s " +
        "LEFT JOIN s.conditions sc " +
        "LEFT JOIN sc.condition c " +
        "WHERE s.hubId = :hubId ")
    List<ScenarioConditionView> findScenarioConditions(@Param("hubId") String hubId);

    @Query("SELECT s.id as scenarioId, " +
            "sa.sensor.id as sensorId, " +
            "s.name as scenarioName, " +
            "a.id as actionId, " +
            "a.type as actionType, " +
            "a.value as actionValue " +
        "FROM Scenario s " +
        "LEFT JOIN s.actions sa " +
        "LEFT JOIN sa.action a " +
        "WHERE s.id IN :scenarioIds ")
    List<ScenarioActionView> findScenarioActions(@Param("scenarioIds") Set<Long> scenarioIds);

    @Query("SELECT s FROM Scenario s WHERE CONCAT(s.hubId, ':', s.name) IN :keys")
    List<Scenario> findByHubIdAndNameIn(@Param("keys") Set<String> keys);
}
