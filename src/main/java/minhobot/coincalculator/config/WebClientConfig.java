package minhobot.coincalculator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

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
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public WebClient telegramWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("telegram")
                .maxIdleTime(Duration.ofSeconds(20))
                .build();

        HttpClient httpClient = HttpClient.create(provider);

        return WebClient.builder()
                .baseUrl("https://api.telegram.org")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}