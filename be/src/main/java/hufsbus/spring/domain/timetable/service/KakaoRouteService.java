package hufsbus.spring.domain.timetable.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KakaoRouteService {

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://apis-navi.kakaomobility.com")
            .build();

    public int getEtaMinutes(double originLat, double originLng,
                              double destLat, double destLng) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/directions")
                            .queryParam("origin", originLng + "," + originLat)
                            .queryParam("destination", destLng + "," + destLat)
                            .build())
                    .header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            int duration = root.path("routes").get(0)
                    .path("summary").path("duration").asInt();
            return duration / 60;
        } catch (Exception e) {
            return -1;
        }
    }
}
