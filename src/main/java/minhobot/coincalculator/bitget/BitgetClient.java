package minhobot.coincalculator.bitget;

import lombok.RequiredArgsConstructor;
import minhobot.coincalculator.bitget.dto.BitgetResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@RequiredArgsConstructor
@Component
public class BitgetClient {

    private final WebClient bitgetWebClient;

    public List<List<String>> getCandles(
            String symbol,
            String granularity,
            String productType,
            int limit
    ) {
        BitgetResponse response = bitgetWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v2/mix/market/candles")
                        .queryParam("symbol", symbol)
                        .queryParam("granularity", granularity)
                        .queryParam("productType", productType)
                        .queryParam("limit", limit)
                        .build()
                )
                .retrieve()
                .bodyToMono(BitgetResponse.class)
                .block();

        if (response != null && "00000".equals(response.getCode())) {
            return response.getData();
        } else {
            throw new RuntimeException("Bitget API error: " + (response != null ? response.getMsg() : "unknown"));
        }
    }
}