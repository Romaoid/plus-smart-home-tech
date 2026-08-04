package analyzer.hub.event.processor.entity;

public interface ScenarioActionView {
    Long getScenarioId();
    String getSensorId();
    String getScenarioName();
    Long getActionId();
    String getActionType();
    Integer getActionValue();
}
