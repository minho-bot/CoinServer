package minhobot.coincalculator.cci;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minhobot.coincalculator.cci.dto.CciResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CciScheduler {

    private final CciService cciService;

    // 1시간봉 체크 (5분 마다 실행)
    @Scheduled(cron = "0 0/5 * * * *")
    public void check1H() {
        checkCci("BTCUSDT", "1H");
    }

    // 4시간봉 체크
    @Scheduled(cron = "0 0/5 * * * *")
    public void check4H() {
        checkCci("BTCUSDT", "4H");
    }

    private void checkCci(String symbol, String granularity) {

        CciResult result = cciService.calculateCci(symbol, granularity);

        double prev = result.getPrevCCI();
        double curr = result.getCurrentCCI();

        String signal = "NONE";

        if (prev < -100 && curr >= -100) {
            signal = "UP_-100";
        } else if (prev > 100 && curr <= 100) {
            signal = "DOWN_100";
        }

        if (!signal.equals("NONE")) {
            log.info("[CCI SIGNAL] {} {} → {}", symbol, granularity, signal);

            // 🔥 다음 단계: 여기에 FCM 푸시 연결하면 됨.
        }
    }
}