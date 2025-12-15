package minhobot.coincalculator.cci;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minhobot.coincalculator.cci.dto.CciResult;
import minhobot.coincalculator.leverage.LeverageResponse;
import minhobot.coincalculator.leverage.LeverageService;
import minhobot.coincalculator.push.ExpoPushClient;
import minhobot.coincalculator.telegram.TelegramBotClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CciScheduler {

    private final CciService cciService;
    private final LeverageService leverageService;
    private final ExpoPushClient expoPushClient;
    private final TelegramBotClient telegramBotClient;

    // 이전 시그널 저장용
    private final Map<String, String> lastSignalMap = new HashMap<>();

    // 1시간봉 체크 (1분 마다 실행)
    @Scheduled(cron = "0 * * * * *")
    public void check1H() {
        checkCci("BTCUSDT", "1H");
    }

    // 4시간봉 체크
    @Scheduled(cron = "0 * * * * *")
    public void check4H() {
        checkCci("BTCUSDT", "4H");
    }

    private void checkCci(String symbol, String granularity) {

        String key = symbol + ":" + granularity;

        CciResult result = cciService.calculateCci(symbol, granularity);

        double prev = result.getPrevCCI();
        double curr = result.getCurrentCCI();
        double price = result.getCurrentPrice();

        String signal = "NONE";

        if (prev < -100 && curr >= -100) {
            signal = "LONG";
        } else if (prev > 100 && curr <= 100) {
            signal = "SHORT";
        }

        // 이전 시그널
        String lastSignal = lastSignalMap.getOrDefault(key, "NONE");

        // 변화가 있을 때만 푸시
        if (!signal.equals("NONE") && !signal.equals(lastSignal)) {
            String title = signal + " Signal (" + granularity + ")";
            String body = granularity + " " + signal + " | Price: " + price;

            log.info("[CCI SIGNAL] {} {} → {}", symbol, granularity, signal);
            // 등록된 계정의 모든 push token 순회
            expoPushClient.sendExpoPush("ExponentPushToken[BOoL4mEJNdCFdxTIkqD2RU]", signal, body);

            LeverageResponse leverageResponse = leverageService.calculateLeverage(symbol, granularity, "USDT-FUTURES", 10, signal.toLowerCase(Locale.ROOT));
            String text = String.format("""
                    test
                    🚨 *CCI SIGNAL DETECTED* 🚨
                    
                    ━━━━━━━━━━━━━━
                    📌 *SYMBOL*      : `%s`
                    📊 *TIMEFRAME*   : `%s`
                    📈 *POSITION*    : *%s*
                    💰 *PRICE*       : `%s`
                    ⚡ *LEVERAGE*    : *%sx*
                    ━━━━━━━━━━━━━━
                    
                    🧠 *Strategy*
                    \\- CCI %s threshold crossover
                    \\- Signal confirmed on close
                    
                    ⏰ *Detected at*
                    `%s`
                    
                    """,
                    symbol,
                    granularity,
                    signal.equals("LONG") ? "🟢 LONG" : "🔴 SHORT",
                    price,
                    leverageResponse.getLeverage(),
                    signal,
                    java.time.LocalDateTime.now()
            );
            telegramBotClient.sendMessage(text);
        }

        // 현재 시그널 저장
        lastSignalMap.put(key, signal);
    }
}