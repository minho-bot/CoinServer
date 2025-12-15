package minhobot.coincalculator.leverage;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/leverage")
public class LeverageController {

    private final LeverageService leverageService;

    @GetMapping
    public LeverageResponse calculateLeverage(
            @RequestParam(defaultValue = "BTCUSDT") String symbol,
            @RequestParam(defaultValue = "1H") String timeframe,
            @RequestParam(defaultValue = "USDT-FUTURES") String productType,
            @RequestParam double lossPercent,
            @RequestParam String side // "long" or "short"
    ) {
        return leverageService.calculateLeverage(symbol, timeframe, productType, lossPercent, side);
    }
}