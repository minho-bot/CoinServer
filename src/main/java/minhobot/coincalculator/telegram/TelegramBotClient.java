package minhobot.coincalculator.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBotClient {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String chatId;

    private final WebClient telegramWebClient;

    public void sendMessage(String text) {
        if (botToken == null || chatId == null) {
            throw new IllegalStateException("TG_BOT_TOKEN or TG_CHAT_ID not set");
        }

        telegramWebClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("chat_id", chatId)
                        .with("text", text))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(res -> log.info("Telegram send success: {}", res))
                .doOnError(err -> log.error("Telegram send failed", err))
                .subscribe();
    }
}
