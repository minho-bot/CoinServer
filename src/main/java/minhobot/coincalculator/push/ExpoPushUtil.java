package minhobot.coincalculator.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpoPushUtil {

    private final WebClient expoWebClient;

    public void sendExpoPush(String expoToken, String title, String body) {

        String payload = """
        {
          "to": "%s",
          "sound": "default",
          "title": "%s",
          "body": "%s"
        }
        """.formatted(expoToken, title, body);

        expoWebClient.post()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("Expo Push Response: {}", res))
                .doOnError(err -> log.error("Expo Push Error: {}", err.getMessage()))
                .subscribe();
    }
}