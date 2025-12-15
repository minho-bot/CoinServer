package minhobot.coincalculator.cci;


import lombok.RequiredArgsConstructor;
import minhobot.coincalculator.bitget.BitgetUtil;
import minhobot.coincalculator.cci.dto.CciResult;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.CCIIndicator;
import org.ta4j.core.num.DecimalNum;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CciService {

    private final BitgetUtil bitgetUtil;

    public CciResult calculateCci(String symbol, String granularity) {

        // 1) Bitget 데이터 받아오기
        List<List<String>> candles = bitgetUtil.getCandles(
                symbol, granularity, "USDT-FUTURES", 21
        );

        // 2) Ta4j BarSeries 생성
        BarSeries series = new BaseBarSeries("cci");
        Duration duration = getDuration(granularity);

        for (List<String> c : candles) {

            long timestamp = Long.parseLong(c.get(0));
            double open = Double.parseDouble(c.get(1));
            double high = Double.parseDouble(c.get(2));
            double low = Double.parseDouble(c.get(3));
            double close = Double.parseDouble(c.get(4));
            double volume = Double.parseDouble(c.get(5));
            double amount = Double.parseDouble(c.get(6));

            Bar bar = new BaseBar(
                    duration,
                    Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("Asia/Seoul")),
                    DecimalNum.valueOf(open),
                    DecimalNum.valueOf(high),
                    DecimalNum.valueOf(low),
                    DecimalNum.valueOf(close),
                    DecimalNum.valueOf(volume),
                    DecimalNum.valueOf(amount)
            );

            series.addBar(bar);
        }

        // 3) CCI(20) 계산
        CCIIndicator cci = new CCIIndicator(series, 20);

        int last = series.getEndIndex();
        int prev = last - 1;

        double prevCCI = cci.getValue(prev).doubleValue();
        double currentCCI = cci.getValue(last).doubleValue();
        double currentPrice = Double.parseDouble(candles.get(candles.size() - 1).get(4));

        return new CciResult(prevCCI, currentCCI, currentPrice);
    }

    private Duration getDuration(String granularity) {
        if (granularity.equals("1H")) return Duration.ofHours(1);
        if (granularity.equals("4H")) return Duration.ofHours(4);
        throw new RuntimeException("unsupported granularity");
    }
}