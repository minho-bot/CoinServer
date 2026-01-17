package minhobot.coincalculator.bitget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import minhobot.coincalculator.cci.RealTimeCciService;
import okhttp3.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BitgetWebSocketClient extends WebSocketListener {

    private final RealTimeCciService realTimeCciService;
    private final ObjectMapper objectMapper;

    private WebSocket webSocket;
    private static final String WS_URL = "wss://ws.bitget.com/v2/ws/public";

    // 연결이 끊기지 않도록 ReadTimeout을 0으로 설정
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();

    @PostConstruct
    public void connect() {
        Request request = new Request.Builder().url(WS_URL).build();
        webSocket = client.newWebSocket(request, this);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        log.info("✅ [WebSocket] 비트겟 V2 서버 연결 성공! 구독을 시작합니다.");
        subscribeCandles();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            // 1. Heartbeat (Pong) 무시
            if ("pong".equalsIgnoreCase(text)) return;

            // 2. JSON 파싱
            JsonNode root = objectMapper.readTree(text);

            // 3. 데이터 수신 (snapshot: 최초로딩, update: 실시간변경)
            if (root.has("action") && (root.get("action").asText().equals("update"))) {

                // 데이터가 비어있지 않은지 확인
                if (root.has("data") && root.get("data").isArray() && !root.get("data").isEmpty()) {

                    // 어떤 봉인지 확인 (candle1H vs candle4H)
                    String channel = root.get("arg").get("channel").asText();
                    String granularity = parseGranularity(channel);

                    // 데이터 배열 파싱
                    JsonNode data = root.get("data").get(0);

                    // 순서: [0]시간, [1]시가, [2]고가, [3]저가, [4]종가, [5]코인거래량
                    long timestamp = Long.parseLong(data.get(0).asText());
                    double open    = Double.parseDouble(data.get(1).asText());
                    double high    = Double.parseDouble(data.get(2).asText());
                    double low     = Double.parseDouble(data.get(3).asText());
                    double close   = Double.parseDouble(data.get(4).asText());
                    double volume  = Double.parseDouble(data.get(5).asText());

                    // 서비스 호출
                    realTimeCciService.updateCandle("BTCUSDT", granularity, timestamp, open, high, low, close, volume);
                }
            }

        } catch (Exception e) {
            log.error("❌ [WebSocket] 메시지 처리 에러: {}", e.getMessage());
        }
    }

    // 채널명 변환 (candle1H -> 1H)
    private String parseGranularity(String channel) {
        if ("candle1H".equals(channel)) return "1H";
        if ("candle4H".equals(channel)) return "4H";
        return "UNKNOWN";
    }

    // 연결 끊기면 재접속
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        log.error("⚠️ [WebSocket] 연결 끊김: {}. 5초 후 재접속...", t.getMessage());
        try {
            Thread.sleep(5000);
            connect();
        } catch (InterruptedException e) {}
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(1000, null);
    }

    // 구독 요청
    private void subscribeCandles() {
        String json = """
                {
                    "op": "subscribe",
                    "args": [
                        {
                            "instType": "USDT-FUTURES",
                            "channel": "candle1H",
                            "instId": "BTCUSDT"
                        },
                        {
                            "instType": "USDT-FUTURES",
                            "channel": "candle4H",
                            "instId": "BTCUSDT"
                        }
                    ]
                }
                """;
        webSocket.send(json);
    }

    // 30초마다 Ping (연결 유지)
    @Scheduled(fixedRate = 30000)
    public void sendPing() {
        if (webSocket != null) webSocket.send("ping");
    }

    @PreDestroy
    public void close() {
        if (webSocket != null) webSocket.close(1000, "Shutdown");
    }
}