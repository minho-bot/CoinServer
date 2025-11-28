package minhobot.coincalculator.push;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ExpoPushController {

    private final ExpoPushService expoPushService;

    @PostMapping("/expo/send")
    public String sendExpo(@RequestBody ExpoPushRequest req) {
        expoPushService.sendExpoPush(req.getToken(), req.getTitle(), req.getBody());
        return "OK";
    }
}