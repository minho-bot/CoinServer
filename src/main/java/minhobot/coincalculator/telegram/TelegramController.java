package minhobot.coincalculator.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramBotClient telegramBotClient;

    @GetMapping("/tg/send")
    public String test() {
        telegramBotClient.sendMessage("spring test");
        return "ok";
    }
}