package minhobot.coincalculator.cci;

import lombok.RequiredArgsConstructor;
import minhobot.coincalculator.cci.dto.CciResponse;
import minhobot.coincalculator.cci.dto.CciResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CciController {

    private final CciService cciService;

    @GetMapping("/cci/check")
    public Object checkCci(
            @RequestParam(defaultValue = "BTCUSDT") String symbol,
            @RequestParam(defaultValue = "1H") String granularity
    ) {
        CciResult result = cciService.calculateCci(symbol, granularity);

        double prev = result.getPrevCCI();
        double curr = result.getCurrentCCI();
        double currentPrice = result.getCurrentPrice();

        String signal = "NONE";

        if (prev < -100 && curr >= -100) signal = "UP_-100";
        if (prev > 100 && curr <= 100) signal = "DOWN_100";

        return new CciResponse(prev, curr, currentPrice, signal);
    }
}