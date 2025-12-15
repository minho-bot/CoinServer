package minhobot.coincalculator.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramBotUtil telegramBotUtil;

    @GetMapping("/tg/send")
    public String test() {
        telegramBotUtil.sendMessage("spring test");
        return "ok";
    }
}