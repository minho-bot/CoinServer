package minhobot.coincalculator.push;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ExpoPushController {

    private final ExpoPushUtil expoPushUtil;

    @PostMapping("/expo/send")
    public String sendExpo(@RequestBody ExpoPushRequest req) {
        expoPushUtil.sendExpoPush(req.getToken(), req.getTitle(), req.getBody());
        return "OK";
    }
}