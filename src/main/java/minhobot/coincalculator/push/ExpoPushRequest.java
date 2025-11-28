package minhobot.coincalculator.push;

import lombok.Getter;

@Getter
public class ExpoPushRequest {
    private String token;
    private String title;
    private String body;
}