package minhobot.coincalculator.leverage;

import lombok.RequiredArgsConstructor;
import minhobot.coincalculator.bitget.BitgetClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeverageService {

    private final BitgetClient bitgetClient;

    @Value("${app.leverage.candle-count}")
    private int candleCount;

    public LeverageResponse calculateLeverage(
            String symbol,
            String timeframe,
            String productType,
            double lossPercent,
            double targetProfitPercent,
            String side
    ) {
        List<List<String>> candles = bitgetClient.getCandles(symbol, timeframe, productType, candleCount);

        if (candles.size() < candleCount) {
            throw new RuntimeException("캔들 개수가 부족합니다.");
        }

        List<String> currentCandle = candles.get(candles.size() - 1);
        double currentPrice = Double.parseDouble(currentCandle.get(4));

        double lowestLow = Double.MAX_VALUE;
        double highestHigh = Double.MIN_VALUE;

        for (List<String> candle : candles) {
            double high = Double.parseDouble(candle.get(2));
            double low = Double.parseDouble(candle.get(3));
            if (low < lowestLow) lowestLow = low;
            if (high > highestHigh) highestHigh = high;
        }

        // --- 여기서부터 분기 시작 ---

        double stoploss;     // 손절가
        double diffRate;     // 진입가와 손절가의 차이 비율 (양수)
        int direction;       // 롱: 1, 숏: -1 (익절가 계산용)

        if (side.equalsIgnoreCase("long")) {
            stoploss = lowestLow;
            diffRate = (currentPrice - stoploss) / currentPrice;
            direction = 1;
        } else if (side.equalsIgnoreCase("short")) {
            stoploss = highestHigh;
            diffRate = (stoploss - currentPrice) / currentPrice;
            direction = -1;
        } else {
            throw new RuntimeException("Side must be LONG or SHORT");
        }

        // --- 공통 로직 (중복 제거됨) ---

        // 1. 방어 로직 (이미 손절가 돌파시)
        if (diffRate <= 0) diffRate = 0.0001;

        // 2. 레버리지 계산
        double lossRatio = lossPercent / 100.0;
        double rawLeverage = lossRatio / diffRate;

        // 3. 레버리지 보정 (1 ~ 125)
        int finalLeverage = (int) Math.floor(rawLeverage);
        if (finalLeverage < 1) finalLeverage = 1;
        if (finalLeverage > 125) finalLeverage = 125;

        // 4. 익절가 계산
        // 목표 수익률(ratio) / 레버리지 = 순수 코인 변동폭
        // 롱(1)이면 더하고, 숏(-1)이면 뺌
        double profitRatio = targetProfitPercent / 100.0;
        double priceMoveNeeded = profitRatio / finalLeverage;

        double takeprofit = currentPrice * (1 + (priceMoveNeeded * direction));

        return LeverageResponse.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .side(side.toUpperCase())
                .currentPrice(currentPrice)
                .leverage(finalLeverage)
                .stopLoss(stoploss)
                .takeProfit(takeprofit)
                .build();
    }
}