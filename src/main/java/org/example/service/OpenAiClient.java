package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAiClient {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private final ObjectMapper mapper = new ObjectMapper();

    public String chat(String systemPrompt, String userPrompt) throws Exception {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("OpenAI API key not configured.");
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", "gpt-4o-mini");
            body.put("messages", new Object[]{
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            });
            body.put("temperature", 0.2);
            body.put("max_tokens", 800);

            String requestBody = mapper.writeValueAsString(body);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("OpenAI error: " + resp.statusCode() + " - " + resp.body());
            }

            JsonNode root = mapper.readTree(resp.body());
            // choices[0].message.content 추출
            JsonNode content = root.path("choices").get(0).path("message").path("content");
            return content.isMissingNode() ? "" : content.asText();
        }
        // 1분에 보낼 수 있는 요청 수는 3회인데 힌트 3회 + 답전송 1회를 1분내로 요청 시 OpenAI API 요청 제한 오류 발생
        // 이를 오류 메시지가 아닌 사용자에게 다른 문구로 대체하여 보내는 코드
        catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                // 429 전용 메시지
                return "현재 AI 서버 요청이 너무 많습니다.\n" +
                        "잠시 후(약 20초 뒤)에 다시 시도해 주세요.";
            }
            throw new RuntimeException(
                    "OpenAI error: " + e.getStatusCode().value() + " - " + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            throw new RuntimeException("OpenAI 호출 중 오류: " + e.getMessage(), e);
        }
    }
}