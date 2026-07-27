package aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Главный класс сервиса Aggregator.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(basePackages = {"aggregator", "tools"})
public class Aggregator {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Aggregator.class, args);

        Runtime.getRuntime().addShutdownHook(new Thread(context::close));

        AggregationStarter aggregator = context.getBean(AggregationStarter.class);
        aggregator.start();
    }
}
