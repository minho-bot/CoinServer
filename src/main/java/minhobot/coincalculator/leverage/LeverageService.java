package minhobot.coincalculator.leverage;

import lombok.RequiredArgsConstructor;
import minhobot.coincalculator.bitget.BitgetClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeverageService {

    private final BitgetClient bitgetClient;

    public LeverageResponse calculateLeverage(
            String symbol,
            String timeframe,
            String productType,
            double lossPercent,
            String side // "long" or "short"
    ) {
        // 최근 2개 봉 가져오기 (현재 + 이전)
        List<List<String>> candles = bitgetClient.getCandles(symbol, timeframe, productType, 2);

        if (candles.size() < 2) {
            throw new RuntimeException("캔들 개수가 부족합니다.");
        }

        List<String> prev = candles.get(0);  // 이전 봉
        List<String> curr = candles.get(1);  // 현재 봉

        // 문자열 -> double 변환
        double currOpen = Double.parseDouble(curr.get(1));
        double currHigh = Double.parseDouble(curr.get(2));
        double currLow  = Double.parseDouble(curr.get(3));
        double currClose = Double.parseDouble(curr.get(4));

        double prevHigh = Double.parseDouble(prev.get(2));
        double prevLow  = Double.parseDouble(prev.get(3));

        double currentPrice = currClose;

        // 계산식
        lossPercent = lossPercent / 100.0; // % -> 0.x

        double leverage;
        double stoploss;

        if (side.equalsIgnoreCase("long")) {
            double worstLow = Math.min(currLow, prevLow);
            double drawdown = (currentPrice - worstLow) / currentPrice;
            leverage = lossPercent / drawdown;
            stoploss = worstLow;
        } else if (side.equalsIgnoreCase("short")) {
            double worstHigh = Math.max(currHigh, prevHigh);
            double drawup = (worstHigh - currentPrice) / currentPrice;
            leverage = lossPercent / drawup;
            stoploss = worstHigh;
        } else {
            throw new RuntimeException("side는 long 또는 short 여야 합니다.");
        }

        int finalLeverage = (int) Math.floor(leverage);

        return LeverageResponse.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .side(side)
                .lossPercent(lossPercent)
                .leverage(finalLeverage)
                .currentPrice(currentPrice)
                .currHigh(currHigh)
                .prevHigh(prevHigh)
                .currLow(currLow)
                .prevLow(prevLow)
                .stoploss(stoploss)
                .build();
    }
}
