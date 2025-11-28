package minhobot.coincalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoinServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinServerApplication.class, args);
    }

}
