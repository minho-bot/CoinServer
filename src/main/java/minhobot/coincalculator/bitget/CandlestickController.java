package minhobot.coincalculator.bitget;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CandlestickController {

    private final BitgetUtil bitgetUtil;

    @GetMapping("/candles")
    public Object getCandles(
            @RequestParam(defaultValue = "BTCUSDT") String symbol,
            @RequestParam(defaultValue = "1H") String granularity,
            @RequestParam(defaultValue = "USDT-FUTURES") String productType,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return bitgetUtil.getCandles(symbol, granularity, productType, limit);
    }
}