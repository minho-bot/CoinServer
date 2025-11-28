package minhobot.coincalculator.cci.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CciResult {
    private double prevCCI;
    private double currentCCI;
    private double currentPrice;
}