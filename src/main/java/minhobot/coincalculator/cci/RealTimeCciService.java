package minhobot.coincalculator.cci;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minhobot.coincalculator.bitget.BitgetClient;
import minhobot.coincalculator.leverage.LeverageResponse;
import minhobot.coincalculator.leverage.LeverageService;
import minhobot.coincalculator.telegram.TelegramBotClient;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.CCIIndicator;
import org.ta4j.core.num.DecimalNum;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeCciService {

    private final BitgetClient bitgetClient;
    private final LeverageService leverageService;
    private final TelegramBotClient telegramBotClient;

    private final Map<String, List<Bar>> chartDataMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAlertedTimeMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadHistory("BTCUSDT", "1H");
        loadHistory("BTCUSDT", "4H");
    }

    private void loadHistory(String symbol, String granularity) {
        try {
            List<List<String>> candles = bitgetClient.getCandles(symbol, granularity, "USDT-FUTURES", 21);

            // 수정 가능한 리스트 생성
            List<Bar> barList = new ArrayList<>();
            Duration duration = getDuration(granularity);

            for (List<String> c : candles) {
                barList.add(parseListToBar(c, duration));
            }

            chartDataMap.put(symbol + ":" + granularity, barList);
            log.info("✅ Init {} {} 완료 ({}개)", symbol, granularity, barList.size());
        } catch (Exception e) {
            log.error("❌ Init 실패: {}", e.getMessage());
        }
    }

    public void updateCandle(String symbol, String granularity, long startTimestamp, double open, double high, double low, double close, double volume) {

        Duration duration = getDuration(granularity);
        ZonedDateTime startTime = Instant.ofEpochMilli(startTimestamp).atZone(ZoneId.of("Asia/Seoul"));
        ZonedDateTime endTime = startTime.plus(duration);

        Bar newBar = new BaseBar(
                duration,
                endTime,
                DecimalNum.valueOf(open),
                DecimalNum.valueOf(high),
                DecimalNum.valueOf(low),
                DecimalNum.valueOf(close),
                DecimalNum.valueOf(volume),
                DecimalNum.valueOf(0)
        );

        processUpdate(symbol, granularity, newBar);
    }

    private void processUpdate(String symbol, String granularity, Bar newBar) {
        String key = symbol + ":" + granularity;
        List<Bar> barList = chartDataMap.get(key);

        if (barList == null || barList.isEmpty()) return;

        Bar lastBar = barList.get(barList.size() - 1);
        long newTimestamp = newBar.getBeginTime().toInstant().toEpochMilli();
        long lastTimestamp = lastBar.getBeginTime().toInstant().toEpochMilli();

        if (newTimestamp == lastTimestamp) {
            // 같은 봉이면 마지막 봉만 교체
            barList.set(barList.size() - 1, newBar);
        } else if (newTimestamp > lastTimestamp) {
            // 새로운 봉이면 봉 추가
            barList.add(newBar);

            // 메모리 관리 (50개 유지)
            if (barList.size() > 50) {
                barList.remove(0);
            }
        }

        // CCI 계산 호출
        calculateAndCheckSignal(barList, symbol, granularity, newTimestamp, newBar.getClosePrice().doubleValue());
    }

    private void calculateAndCheckSignal(List<Bar> barList, String symbol, String granularity, long currentTimestamp, double currentPrice) {
        if (barList.size() < 21) return;

        // CCI 계산 임시 BarSeries
        BarSeries series = new BaseBarSeries("temp");
        for (Bar bar : barList) {
            series.addBar(bar);
        }

        CCIIndicator cci = new CCIIndicator(series, 20);
        int idx = series.getEndIndex();

        double prevCCI = cci.getValue(idx - 1).doubleValue();
        double currCCI = cci.getValue(idx).doubleValue();

        String signal = "NONE";
        if (prevCCI < -100 && currCCI >= -100) signal = "LONG";
        else if (prevCCI > 100 && currCCI <= 100) signal = "SHORT";

        if ("NONE".equals(signal)) return;

        String mapKey = symbol + ":" + granularity;
        Long lastAlert = lastAlertedTimeMap.get(mapKey);
        if (lastAlert != null && lastAlert == currentTimestamp) return;

        sendAlert(symbol, granularity, signal, currentPrice, currentTimestamp);
    }

    private void sendAlert(String symbol, String granularity, String signal, double price, long timestamp) {
        log.info("🚀 [BREAKOUT] {} {} -> {}", granularity, signal, price);
        LeverageResponse leverage = leverageService.calculateLeverage(symbol, granularity, "USDT-FUTURES", 10, 20, signal.toLowerCase(Locale.ROOT));

        telegramBotClient.sendSignalMessage(symbol, granularity, signal, price, leverage);

        lastAlertedTimeMap.put(symbol + ":" + granularity, timestamp);
    }

    private Bar parseListToBar(List<String> c, Duration duration) {
        long timestamp = Long.parseLong(c.get(0));
        ZonedDateTime startTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("Asia/Seoul"));
        ZonedDateTime endTime = startTime.plus(duration);

        return new BaseBar(
                duration,
                endTime,
                DecimalNum.valueOf(c.get(1)),
                DecimalNum.valueOf(c.get(2)),
                DecimalNum.valueOf(c.get(3)),
                DecimalNum.valueOf(c.get(4)),
                DecimalNum.valueOf(c.get(5)),
                DecimalNum.valueOf(0)
        );
    }

    private Duration getDuration(String granularity) {
        return "4H".equals(granularity) ? Duration.ofHours(4) : Duration.ofHours(1);
    }
}