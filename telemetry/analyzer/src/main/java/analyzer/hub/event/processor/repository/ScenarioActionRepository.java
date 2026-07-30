package analyzer.hub.event.processor.repository;

import analyzer.hub.event.processor.entity.ScenarioAction;
import analyzer.hub.event.processor.entity.ScenarioActionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, ScenarioActionId> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ScenarioAction sa WHERE sa.scenario.id = :scenarioId")
    void deleteAllByScenarioId(@Param("scenarioId") Long scenarioId);

    @Query("SELECT sa.action.id FROM ScenarioAction sa " +
            "LEFT JOIN ScenarioAction sa2 ON sa.action.id = sa2.action.id " +
            "WHERE sa2.action.id IS NULL")
    List<Long> findOrphanActionIds();
}
