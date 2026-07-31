package analyzer.hub.event.processor.repository;

import analyzer.hub.event.processor.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface SensorRepository extends JpaRepository<Sensor, String> {
    List<Sensor> findAllByIdIn(Set<String> ids);

    @Query("SELECT s.id FROM Sensor s WHERE s.id IN :ids")
    Set<String> findExistingIds(@Param("ids") Set<String> ids);

    @Query("SELECT s.id FROM Sensor s WHERE s.id IN :ids AND s.hubId = :hubId")
    Set<String> findExistingIdsByHubId(@Param("ids") Set<String> ids,
                                       @Param("hubId") String hubId);

    @Query("SELECT s FROM Sensor s WHERE s.id IN :ids AND s.hubId = :hubId")
    List<Sensor> findAllByIdInAndHubId(@Param("ids") Set<String> ids,
                                       @Param("hubId") String hubId);
}