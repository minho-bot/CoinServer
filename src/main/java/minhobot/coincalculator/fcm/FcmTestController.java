package minhobot.coincalculator.fcm;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FcmTestController {

    private final FcmService fcmService;

    @PostMapping("/fcm/test")
    public String testPush(
            @RequestParam String token,
            @RequestParam(defaultValue = "Test Title") String title,
            @RequestParam(defaultValue = "Test Body") String body
    ) {
        fcmService.send(token, title, body);
        return "Push sent!";
    }
}