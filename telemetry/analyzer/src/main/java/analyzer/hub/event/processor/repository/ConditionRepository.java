package analyzer.hub.event.processor.repository;

import analyzer.hub.event.processor.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ConditionRepository extends JpaRepository<Condition, Long> {
    Optional<Condition> findByTypeAndOperationAndValue(
            ConditionTypeAvro type,
            ConditionOperationAvro operation,
            Integer value
    );

    @Query("SELECT c FROM Condition c WHERE CONCAT(c.type, ':', c.operation, ':', c.value) IN :keys")
    List<Condition> findByTypeOperationValueIn(@Param("keys") Set<String> keys);
}
