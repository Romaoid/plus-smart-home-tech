package telcol.dto.hub.scenario.added;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ScenarioCondition {
    @NotBlank
    private String sensorId;
    @NotNull
    private ScenarioConditionType type;
    @NotNull
    private ScenarioConditionOperation operation;
    @NotNull
    private int value;
}
