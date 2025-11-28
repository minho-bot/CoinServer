package minhobot.coincalculator.leverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LeverageResponse {

    private String symbol;
    private String timeframe;
    private String side;
    private double lossPercent;
    private int leverage;

    private double currentPrice;

    private double currHigh;
    private double prevHigh;
    private double currLow;
    private double prevLow;
}