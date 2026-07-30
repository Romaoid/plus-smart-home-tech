package analyzer.hub.event.processor.entity;

public interface ScenarioConditionView {
    Long getScenarioId();
    String getHubId();
    String getScenarioName();
    String getSensorId();
    Long getConditionId();
    String getConditionType();
    String getConditionOperation();
    Integer getConditionValue();
}
