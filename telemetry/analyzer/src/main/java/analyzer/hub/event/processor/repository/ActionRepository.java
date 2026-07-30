package analyzer.hub.event.processor.repository;

import analyzer.hub.event.processor.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;

import java.util.Optional;

public interface ActionRepository extends JpaRepository<Action, Long> {
    Optional<Action> findByTypeAndValue(ActionTypeAvro type, Integer value);
}