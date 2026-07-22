package telcol.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.*;
import telcol.model.hub.HubEventHandler;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
public class EventController extends CollectorControllerGrpc.CollectorControllerImplBase {
    private final Map<HubEventProto.PayloadCase, HubEventHandler> hubEventHandlers;

    public EventController(Set<HubEventHandler> hubEventHandlers) {
        this.hubEventHandlers = hubEventHandlers.stream()
                .collect(Collectors.toMap(
                        HubEventHandler::getMessageType,
                        Function.identity()
                ));
    }

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            SensorEventProto.PayloadCase payloadCase = request.getPayloadCase();
            switch (payloadCase) {
                case LIGHT_SENSOR:
                    System.out.println("Получено событие датчика освещённости");
                    LightSensorProto lightSensor = request.getLightSensor();
                    System.out.println("Уровень освещённости: " + lightSensor.getLuminosity());
                    break;
                case CLIMATE_SENSOR:
                    System.out.println("Получено событие климатического датчика");
                    ClimateSensorProto climateSensor = request.getClimateSensor();
                    System.out.println("Влажность воздуха: " + climateSensor.getHumidity());
                    break;
                case MOTION_SENSOR:
                    System.out.println("Получено событие датчика движения");
                    MotionSensorProto motionSensor = request.getMotionSensor();
                    System.out.println("Движение зафиксировано: " + motionSensor.getMotion());
                    break;
                case SWITCH_SENSOR:
                    System.out.println("Получено событие датчика включения");
                    SwitchSensorProto switchSensor = request.getSwitchSensor();
                    System.out.println("Включено: " + switchSensor.getState());
                    break;
                case TEMPERATURE_SENSOR:
                    System.out.println("Получено событие датчика температуры");
                    TemperatureSensorProto temperatureSensor = request.getTemperatureSensor();
                    System.out.println("Температура воздуха: " + temperatureSensor.getTemperatureC());
                    break;
                default:
                    System.out.println("Получено событие неизвестного типа: " + payloadCase);
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            if (hubEventHandlers.containsKey(request.getPayloadCase())) {
                hubEventHandlers.get(request.getPayloadCase()).handle(request);
            } else {
                throw new IllegalArgumentException("Не могу найти обработчик для события " + request.getPayloadCase());
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }
}
