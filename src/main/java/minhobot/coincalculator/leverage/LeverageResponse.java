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
    private double currentPrice;
    private int leverage;
    private double stopLoss;
    private double takeProfit;
}