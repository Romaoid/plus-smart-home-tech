package analyzer.hub.event.processor.repository;

import analyzer.hub.event.processor.entity.ScenarioCondition;
import analyzer.hub.event.processor.entity.ScenarioConditionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioConditionId> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ScenarioCondition sc WHERE sc.scenario.id = :scenarioId")
    void deleteAllByScenarioId(@Param("scenarioId") Long scenarioId);

    @Query("SELECT sc.condition.id FROM ScenarioCondition sc " +
            "LEFT JOIN ScenarioCondition sc2 ON sc.condition.id = sc2.condition.id " +
            "WHERE sc2.condition.id IS NULL")
    List<Long> findOrphanConditionIds();
}
