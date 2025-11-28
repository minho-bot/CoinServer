package minhobot.coincalculator.bitget.dto;

import lombok.Data;

import java.util.List;

@Data
public class BitgetResponse {
    private String code;
    private String msg;
    private long requestTime;
    private List<List<String>> data;
}