package minhobot.coincalculator.push;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ExpoPushController {

    private final ExpoPushClient expoPushClient;

    @PostMapping("/expo/send")
    public String sendExpo(@RequestBody ExpoPushRequest req) {
        expoPushClient.sendExpoPush(req.getToken(), req.getTitle(), req.getBody());
        return "OK";
    }
}