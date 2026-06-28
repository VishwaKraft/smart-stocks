package com.smartstocks.product.service;

import java.util.Objects;

import com.smartstocks.product.dto.GeminiResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com}")
    private String apiUrl;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String defaultModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private String getApiEndpoint(String model) {
        return apiUrl + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
    }

    private JsonObject createTextPart(String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        return part;
    }

    private JsonObject createContentObject(String role, String text) {
        JsonObject content = new JsonObject();
        content.addProperty("role", role);
        JsonArray parts = new JsonArray();
        parts.add(createTextPart(text));
        content.add("parts", parts);
        return content;
    }

    /**
     * Basic method for generating responses without function calling or JSON schema
     * validation.
     * Uses the default model (gemini-2.0-flash).
     * 
     * @param systemPrompt The system instructions for the AI
     * @param userMessage  The user's input message
     * @return GeminiResponse containing the AI's response
     */
    public GeminiResponse generateBasicChatResponse(String systemPrompt, String userMessage) {
        return generateChatResponseWithModel(systemPrompt, userMessage, null);
    }

    /**
     * Generate responses with a specific model.
     * Use this when you need to specify a different model than the default.
     * 
     * @param systemPrompt The system instructions for the AI
     * @param userMessage  The user's input message
     * @param model        The specific Gemini model to use (e.g.,
     *                     "gemini-2.0-flash", "gemini-1.5-pro")
     * @return GeminiResponse containing the AI's response
     */
    public GeminiResponse generateChatResponseWithModel(String systemPrompt, String userMessage, String model) {
        return generateResponseInternal(systemPrompt, userMessage, null, null, model);
    }

    /**
     * Generate responses with function calling capabilities.
     * Use this when you need the AI to call specific functions as part of its
     * response.
     * Uses the default model.
     * 
     * @param systemPrompt The system instructions for the AI
     * @param userMessage  The user's input message
     * @param functions    Array of function definitions the AI can call
     * @return GeminiResponse containing the AI's response or function call
     */
    public GeminiResponse generateFunctionCallResponse(String systemPrompt, String userMessage,
            JsonArray functions) {
        return generateFunctionCallResponseWithModel(systemPrompt, userMessage, functions, null);
    }

    /**
     * Generate responses with function calling capabilities and specific model.
     * Use this when you need both function calling and a specific model.
     * 
     * @param systemPrompt The system instructions for the AI
     * @param userMessage  The user's input message
     * @param functions    Array of function definitions the AI can call
     * @param model        The specific Gemini model to use
     * @return GeminiResponse containing the AI's response or function call
     */
    public GeminiResponse generateFunctionCallResponseWithModel(String systemPrompt, String userMessage,
            JsonArray functions, String model) {
        return generateResponseInternal(systemPrompt, userMessage, functions, null, model);
    }

    /**
     * Generate responses with JSON schema validation.
     * Use this when you need the AI's response to conform to a specific JSON
     * structure.
     * Uses the default model.
     * 
     * @param systemPrompt The system instructions for the AI
     * @param userMessage  The user's input message
     * @param jsonSchema   The JSON schema that the response must conform to
     * @return GeminiResponse containing the AI's JSON-structured response
     */
    public GeminiResponse generateStructuredJsonResponse(String systemPrompt, String userMessage,
            JsonObject jsonSchema) {
        return generateStructuredJsonResponseWithModel(systemPrompt, userMessage, jsonSchema, null);
    }

    /**
     * Generate responses with JSON schema validation and specific model.
     * Use this when you need both JSON schema validation and a specific model.
     * 
     * @param systemPrompt The system instructions for the AI
     * @param userMessage  The user's input message
     * @param jsonSchema   The JSON schema that the response must conform to
     * @param model        The specific Gemini model to use
     * @return GeminiResponse containing the AI's JSON-structured response
     */
    public GeminiResponse generateStructuredJsonResponseWithModel(String systemPrompt, String userMessage,
            JsonObject jsonSchema, String model) {
        return generateResponseInternal(systemPrompt, userMessage, null, jsonSchema, model);
    }

    private GeminiResponse generateResponseInternal(String systemPrompt, String userMessage,
            JsonArray functions, JsonObject jsonSchema, String model) {

        model = (model != null) ? model : defaultModel;

        JsonObject requestBody = new JsonObject();

        // Add system instruction
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemInstruction = new JsonObject();
            JsonArray systemParts = new JsonArray();
            systemParts.add(createTextPart(systemPrompt));
            systemInstruction.add("parts", systemParts);
            requestBody.add("systemInstruction", systemInstruction);
        }

        // Add contents (user message)
        JsonArray contents = new JsonArray();
        contents.add(createContentObject("user", userMessage));
        requestBody.add("contents", contents);

        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 1);
        generationConfig.addProperty("topK", 40);
        generationConfig.addProperty("topP", 0.95);
        generationConfig.addProperty("maxOutputTokens", 8192);

        // Add JSON schema response format if provided
        if (jsonSchema != null) {
            generationConfig.addProperty("responseMimeType", "application/json");
            generationConfig.add("responseSchema", jsonSchema);
        }

        requestBody.add("generationConfig", generationConfig);

        // Add function declarations if provided
        if (functions != null && functions.size() > 0) {
            JsonArray tools = new JsonArray();
            JsonObject functionDeclarations = new JsonObject();
            functionDeclarations.add("functionDeclarations", functions);
            tools.add(functionDeclarations);
            requestBody.add("tools", tools);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(getApiEndpoint(model), HttpMethod.POST, entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonObject jsonResponse = new JsonParser().parse(Objects.requireNonNull(response.getBody()))
                        .getAsJsonObject();

                JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                JsonObject content = candidate.get("content").getAsJsonObject();
                JsonArray parts = content.getAsJsonArray("parts");

                String textContent = null;
                String functionName = null;
                String functionArgs = null;
                JsonNode parsedContent = null;

                // Check if response contains function call
                if (parts.get(0).getAsJsonObject().has("functionCall")) {
                    JsonObject functionCall = parts.get(0).getAsJsonObject().get("functionCall").getAsJsonObject();
                    functionName = functionCall.get("name").getAsString();
                    functionArgs = functionCall.get("args").toString();
                } else {
                    // Extract text content
                    textContent = parts.get(0).getAsJsonObject().get("text").getAsString();

                    // Parse JSON content if schema was provided
                    if (jsonSchema != null && textContent != null) {
                        try {
                            parsedContent = objectMapper.readTree(textContent);
                        } catch (Exception e) {
                            logger.error("Error parsing content as JSON", e);
                        }
                    }
                }

                // Extract usage metadata
                JsonObject usageMetadata = jsonResponse.has("usageMetadata")
                        ? jsonResponse.getAsJsonObject("usageMetadata")
                        : new JsonObject();

                int promptTokens = usageMetadata.has("promptTokenCount")
                        ? usageMetadata.get("promptTokenCount").getAsInt()
                        : 0;
                int completionTokens = usageMetadata.has("candidatesTokenCount")
                        ? usageMetadata.get("candidatesTokenCount").getAsInt()
                        : 0;
                int totalTokens = usageMetadata.has("totalTokenCount")
                        ? usageMetadata.get("totalTokenCount").getAsInt()
                        : 0;

                String finishReason = candidate.has("finishReason")
                        ? candidate.get("finishReason").getAsString()
                        : "STOP";

                return GeminiResponse.builder()
                        .message(textContent)
                        .functionName(functionName)
                        .functionArgs(functionArgs)
                        .parsedContent(parsedContent)
                        .hasErrors(false)
                        .statusCode(response.getStatusCode().value())
                        .model(model)
                        .promptTokens(promptTokens)
                        .completionTokens(completionTokens)
                        .totalTokens(totalTokens)
                        .finishReason(finishReason)
                        .createdTimestamp(startTime / 1000)
                        .build();
            } else {
                return GeminiResponse.builder()
                        .message("Error: " + response.getStatusCode() + " - " + response.getBody())
                        .hasErrors(true)
                        .statusCode(response.getStatusCode().value())
                        .build();
            }
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error calling Gemini API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            String errorMessage = e.getResponseBodyAsString();
            if (errorMessage.contains("quota") || errorMessage.contains("RESOURCE_EXHAUSTED")) {
                errorMessage = "API quota exceeded. Please try again later.";
            }
            return GeminiResponse.builder()
                    .message(errorMessage)
                    .hasErrors(true)
                    .statusCode(e.getStatusCode().value())
                    .build();
        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            return GeminiResponse.builder()
                    .message("Error: " + e.getMessage())
                    .hasErrors(true)
                    .statusCode(500)
                    .build();
        }
    }

    /**
     * Create a function definition for Gemini function calling.
     * 
     * @param name        The name of the function
     * @param description Description of what the function does
     * @param parameters  JSON object defining the function parameters
     * @return JsonObject representing the function definition
     */
    public JsonObject createFunction(String name, String description, JsonObject parameters) {
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", parameters);
        return function;
    }

    /**
     * Create a JSON schema for structured output.
     * 
     * @param schemaContent The JSON schema content
     * @return JsonObject representing the schema
     */
    public JsonObject createJsonSchema(JsonObject schemaContent) {
        return schemaContent;
    }

    /**
     * Generate a response with multi-turn conversation history.
     * 
     * @param systemPrompt        The system instructions for the AI
     * @param conversationHistory Array of previous conversation turns
     * @param userMessage         The current user message
     * @param model               The specific Gemini model to use (optional)
     * @return GeminiResponse containing the AI's response
     */
    public GeminiResponse generateChatResponseWithHistory(String systemPrompt,
            JsonArray conversationHistory, String userMessage, String model) {

        model = (model != null) ? model : defaultModel;

        JsonObject requestBody = new JsonObject();

        // Add system instruction
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemInstruction = new JsonObject();
            JsonArray systemParts = new JsonArray();
            systemParts.add(createTextPart(systemPrompt));
            systemInstruction.add("parts", systemParts);
            requestBody.add("systemInstruction", systemInstruction);
        }

        // Add conversation history and current message
        JsonArray contents = new JsonArray();
        if (conversationHistory != null) {
            contents.addAll(conversationHistory);
        }
        contents.add(createContentObject("user", userMessage));
        requestBody.add("contents", contents);

        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 1);
        generationConfig.addProperty("topK", 40);
        generationConfig.addProperty("topP", 0.95);
        generationConfig.addProperty("maxOutputTokens", 8192);
        requestBody.add("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(getApiEndpoint(model), HttpMethod.POST, entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonObject jsonResponse = new JsonParser().parse(Objects.requireNonNull(response.getBody()))
                        .getAsJsonObject();

                JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
                JsonObject content = candidate.get("content").getAsJsonObject();
                JsonArray parts = content.getAsJsonArray("parts");

                String textContent = parts.get(0).getAsJsonObject().get("text").getAsString();

                JsonObject usageMetadata = jsonResponse.has("usageMetadata")
                        ? jsonResponse.getAsJsonObject("usageMetadata")
                        : new JsonObject();

                int promptTokens = usageMetadata.has("promptTokenCount")
                        ? usageMetadata.get("promptTokenCount").getAsInt()
                        : 0;
                int completionTokens = usageMetadata.has("candidatesTokenCount")
                        ? usageMetadata.get("candidatesTokenCount").getAsInt()
                        : 0;
                int totalTokens = usageMetadata.has("totalTokenCount")
                        ? usageMetadata.get("totalTokenCount").getAsInt()
                        : 0;

                String finishReason = candidate.has("finishReason")
                        ? candidate.get("finishReason").getAsString()
                        : "STOP";

                return GeminiResponse.builder()
                        .message(textContent)
                        .hasErrors(false)
                        .statusCode(response.getStatusCode().value())
                        .model(model)
                        .promptTokens(promptTokens)
                        .completionTokens(completionTokens)
                        .totalTokens(totalTokens)
                        .finishReason(finishReason)
                        .createdTimestamp(startTime / 1000)
                        .build();
            } else {
                return GeminiResponse.builder()
                        .message("Error: " + response.getStatusCode() + " - " + response.getBody())
                        .hasErrors(true)
                        .statusCode(response.getStatusCode().value())
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error calling Gemini API with history", e);
            return GeminiResponse.builder()
                    .message("Error: " + e.getMessage())
                    .hasErrors(true)
                    .statusCode(500)
                    .build();
        }
    }
}