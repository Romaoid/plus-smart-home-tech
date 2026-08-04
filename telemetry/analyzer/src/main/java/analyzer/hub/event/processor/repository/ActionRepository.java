package analyzer.hub.event.processor.repository;

import analyzer.hub.event.processor.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ActionRepository extends JpaRepository<Action, Long> {
    Optional<Action> findByTypeAndValue(ActionTypeAvro type, Integer value);

    @Query("SELECT a FROM Action a WHERE CONCAT(a.type, ':', a.value) IN :keys")
    List<Action> findByTypeValueIn(@Param("keys") Set<String> keys);
}