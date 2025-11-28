package minhobot.coincalculator.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void send(String token, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .putData("title", title)
                    .putData("body", body)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);

            log.info("FCM sent successfully: {}", response);

        } catch (Exception e) {
            log.error("FCM send failed: {}", e.getMessage());
        }
    }
}