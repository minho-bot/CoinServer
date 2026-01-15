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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class CciScheduler {

    private final CciService cciService;
    private final LeverageService leverageService;
    private final ExpoPushClient expoPushClient;
    private final TelegramBotClient telegramBotClient;

    // 마지막으로 알림을 보낸 '봉의 시작 시간'을 저장
    // Key: "BTCUSDT:1H", Value: 2024-01-15T14:00:00 (해당 봉의 시간)
    private final Map<String, LocalDateTime> lastAlertedTimeMap = new ConcurrentHashMap<>();

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

        // CCI 전략 로직
        if (prev < -100 && curr >= -100) {
            signal = "LONG";
        } else if (prev > 100 && curr <= 100) {
            signal = "SHORT";
        }

        // 시그널이 없으면 아무것도 안 함
        if (signal.equals("NONE")) {
            return;
        }

        // (핵심 로직) 현재 시점의 봉(Candle) 시작 시간 계산
        LocalDateTime currentCandleTime = getCurrentCandleTime(granularity);

        // 마지막으로 알림을 보낸 봉의 시간 가져오기
        LocalDateTime lastAlertedTime = lastAlertedTimeMap.get(key);

        // 이미 이번 봉(currentCandleTime)에서 알림을 보냈다면 스킵 (중복 방지)
        if (lastAlertedTime != null && lastAlertedTime.equals(currentCandleTime)) {
            // 로그 확인용 (필요 없으면 삭제 가능)
            // log.info("이미 {} 봉에 대한 알림을 전송했습니다. 스킵합니다.", currentCandleTime);
            return;
        }

        // --- 여기서부터 알림 전송 로직 ---

        String title = signal + " Signal (" + granularity + ")";
        String body = granularity + " " + signal + " | Price: " + price;

        log.info("[CCI SIGNAL] {} {} → {}", symbol, granularity, signal);

        // 알림 전송
        expoPushClient.sendExpoPush("ExponentPushToken[BOoL4mEJNdCFdxTIkqD2RU]", signal, body);

        LeverageResponse leverageResponse = leverageService.calculateLeverage(symbol, granularity, "USDT-FUTURES", 10, signal.toLowerCase(Locale.ROOT));
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
                \\- recommend stop loss : `%s`
                
                ⏰ *Detected at*
                `%s`
                
                """,
                symbol,
                granularity,
                signal.equals("LONG") ? "🟢 LONG" : "🔴 SHORT",
                price,
                leverageResponse.getLeverage(),
                leverageResponse.getStoploss(),
                LocalDateTime.now()
        );
        telegramBotClient.sendMessage(text);

        // (중요) 알림을 보냈으므로, 현재 봉 시간을 맵에 저장해서 락을 검
        lastAlertedTimeMap.put(key, currentCandleTime);
    }

    // 타임프레임에 따른 현재 봉의 시작 시간 계산
    private LocalDateTime getCurrentCandleTime(String granularity) {
        LocalDateTime now = LocalDateTime.now();

        if ("1H".equals(granularity)) {
            return now.withMinute(0).withSecond(0).withNano(0);
        } else if ("4H".equals(granularity)) {
            // 1, 5, 9, 13, 17, 21시 기준으로 변경
            int hour = now.getHour();

            if (hour < 1) {
                // 00시~00시59분은 사이클상 '전날 21시' 봉에 해당함
                // 예: 1월 2일 00:30 -> 1월 1일 21:00 봉
                return now.minusDays(1).withHour(21).withMinute(0).withSecond(0).withNano(0);
            }

            // 공식: (시간 - 1) / 4 * 4 + 1
            // 예: 2시 -> (1)/4*4 + 1 = 1시
            // 예: 5시 -> (4)/4*4 + 1 = 5시
            int startHour = ((hour - 1) / 4) * 4 + 1;
            return now.withHour(startHour).withMinute(0).withSecond(0).withNano(0);
        }

        return now;
    }
}