package analyzer.hub.event.processor.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "sensors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {
    @Id
    private String id;

    @Column(name = "hub_id")
    private String hubId;
}
