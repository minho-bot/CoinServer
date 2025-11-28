package minhobot.coincalculator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient bitgetWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.bitget.com")
                .build();
    }

    @Bean
    public WebClient expoWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://exp.host/--/api/v2/push/send")
                .build();
    }
}