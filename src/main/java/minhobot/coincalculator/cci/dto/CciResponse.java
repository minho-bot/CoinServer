package minhobot.coincalculator.cci.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CciResponse {
    private double prevCCI;
    private double currentCCI;
    private String crossing;
}