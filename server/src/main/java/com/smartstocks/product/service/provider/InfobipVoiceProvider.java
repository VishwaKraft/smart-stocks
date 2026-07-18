package com.smartstocks.product.service.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends a voice (TTS) message via the Infobip /tts/3/advanced API.
 *
 * API reference: https://www.infobip.com/docs/voice-and-video/text-to-speech
 *
 * Example curl:
 * curl --location 'https://api.infobip.com/tts/3/advanced' \
 *   --header 'Authorization: App <key>' \
 *   --header 'Content-Type: application/json' \
 *   --data '{ "messages": [{ "destinations": [{"to": "918447629515"}],
 *              "from": "38515507799", "language": "en", "text": "Hello",
 *              "voice": {"name": "Joanna", "gender": "female"} }] }'
 */
@Slf4j
public class InfobipVoiceProvider {

    private final String apiKey;
    private final String baseUrl;
    private final RestTemplate restTemplate;

    public InfobipVoiceProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Sends a TTS voice call to the given phone number.
     *
     * @param toPhone      recipient phone number in international format (e.g. "918447629515")
     * @param fromNumber   sender/caller number (e.g. "38515507799")
     * @param messageText  the text to be spoken
     * @param language     BCP-47 language code (e.g. "en", "hi")
     * @param voiceName    Infobip voice name (e.g. "Joanna", "Matthew")
     * @param voiceGender  "female" or "male"
     * @return SendResult indicating success or failure
     */
    public SendResult sendVoice(String toPhone, String fromNumber, String messageText,
                                 String language, String voiceName, String voiceGender) {
        String url = baseUrl + "/tts/3/advanced";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "App " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        Map<String, Object> destination = new HashMap<>();
        destination.put("to", toPhone);

        Map<String, Object> voice = new HashMap<>();
        voice.put("name", voiceName != null ? voiceName : "Joanna");
        voice.put("gender", voiceGender != null ? voiceGender : "female");

        Map<String, Object> message = new HashMap<>();
        message.put("destinations", List.of(destination));
        message.put("from", fromNumber);
        message.put("language", language != null ? language : "en");
        message.put("text", messageText);
        message.put("voice", voice);

        Map<String, Object> body = new HashMap<>();
        body.put("messages", List.of(message));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            log.info("[InfobipVoiceProvider] Voice call queued for [{}]: status={}", toPhone, response.getStatusCode());

            // Extract message ID from response if available
            String messageId = extractMessageId(response.getBody());
            return SendResult.ok(1, messageId != null ? messageId : "queued");

        } catch (HttpClientErrorException ex) {
            log.error("[InfobipVoiceProvider] HTTP error sending voice to [{}]: status={}, body={}",
                    toPhone, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            return SendResult.failure("HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("[InfobipVoiceProvider] Unexpected error sending voice to [{}]: {}", toPhone, ex.getMessage(), ex);
            return SendResult.failure("Exception: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map responseBody) {
        if (responseBody == null) return null;
        try {
            Object messages = responseBody.get("messages");
            if (messages instanceof List && !((List<?>) messages).isEmpty()) {
                Object first = ((List<?>) messages).get(0);
                if (first instanceof Map) {
                    Object id = ((Map<?, ?>) first).get("messageId");
                    return id != null ? id.toString() : null;
                }
            }
        } catch (Exception e) {
            log.debug("[InfobipVoiceProvider] Could not extract messageId from response: {}", e.getMessage());
        }
        return null;
    }
}
