package telcol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {"telcol", "tools"})
public class EventCollector {

    public static void main(String[] args) {
        SpringApplication.run(EventCollector.class, args);
    }

}
