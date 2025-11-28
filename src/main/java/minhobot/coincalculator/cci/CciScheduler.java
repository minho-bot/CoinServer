package minhobot.coincalculator.cci;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minhobot.coincalculator.cci.dto.CciResult;
import minhobot.coincalculator.push.ExpoPushService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CciScheduler {

    private final CciService cciService;
    private final ExpoPushService expoPushService;

    // 이전 시그널 저장용
    private final Map<String, String> lastSignalMap = new HashMap<>();

    // 1시간봉 체크 (5분 마다 실행)
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
            expoPushService.sendExpoPush("ExponentPushToken[BOoL4mEJNdCFdxTIkqD2RU]", signal, granularity + " " + signal);
        }

        // 현재 시그널 저장
        lastSignalMap.put(key, signal);
    }
}