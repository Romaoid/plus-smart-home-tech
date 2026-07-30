package analyzer.snapshot.processor.grps.sender;

import com.google.protobuf.Empty;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@Service
@Slf4j
public class AnalyzerSender {
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub sender;

    public AnalyzerSender(@GrpcClient("hub-router")
                          HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient) {
        this.sender = hubRouterClient;
    }

    public void send(DeviceActionRequest rew) {
        log.info("sending ActionRequest to HubRouter");

        Empty response = sender.handleDeviceAction(rew);
        log.info("response received");
    }
}
