package analyzer.hub.event.processor.repository;

import analyzer.hub.event.processor.entity.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;

import java.util.Optional;

public interface ConditionRepository extends JpaRepository<Condition, Long> {
    Optional<Condition> findByTypeAndOperationAndValue(
            ConditionTypeAvro type,
            ConditionOperationAvro operation,
            Integer value
    );
}
