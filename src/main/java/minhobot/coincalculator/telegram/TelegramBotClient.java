package minhobot.coincalculator.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minhobot.coincalculator.leverage.LeverageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

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
                        .with("text", text)
                        .with("parse_mode", "MarkdownV2"))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("Telegram HTTP {} body={}", resp.statusCode().value(), body);
                                    return reactor.core.publisher.Mono.error(
                                            new RuntimeException("Telegram API error " + resp.statusCode().value() + ": " + body + ", text: " + text)
                                    );
                                })
                )
                .bodyToMono(String.class)
                .subscribe(
                        ok -> log.info("Telegram API response {}", ok),
                        err -> log.error("Telegram subscribe error", err)
                );
    }

    public void sendSignalMessage(String symbol, String granularity, String signal, double price, LeverageResponse leverage) {
        String text = String.format("""
                🚨 *CCI SIGNAL DETECTED* 🚨
                
                ━━━━━━━━━━━━━━
                📌 *SYMBOL* : `%s`
                📊 *TIMEFRAME* : `%s`
                📈 *POSITION* : *%s*
                💰 *PRICE* : `%s`
                ⚡ *LEVERAGE* : *%sx*
                ━━━━━━━━━━━━━━
                
                🧠 *Strategy*
                \\- Stop Loss     : `%s`
                \\- Take Profit   : `%s`
                
                ⏰ *Detected at*
                `%s`
                
                """,
                symbol,
                granularity,
                signal.equals("LONG") ? "🟢 LONG" : "🔴 SHORT",
                price,
                leverage.getLeverage(),
                leverage.getStopLoss(),
                leverage.getTakeProfit(),
                LocalDateTime.now()
        );
        this.sendMessage(text);
    }
}
