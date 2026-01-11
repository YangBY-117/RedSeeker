package com.redseeker.recommend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ZhipuAiServiceImpl implements AiService {

    @Value("${zhipu.api.key:}")
    private String apiKey;

    private static final String MODEL = Constants.ModelChatGLM4;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateContent(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Error: API Key not configured. Please set zhipu.api.key in application.yml";
        }

        ClientV4 client = new ClientV4.Builder(apiKey).build();

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);
        messages.add(chatMessage);

        String requestId = String.format("request-%d", System.currentTimeMillis());

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(MODEL)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .requestId(requestId)
                .build();

        ModelApiResponse invokeModelApiResp = client.invokeModelApi(chatCompletionRequest);

        if (invokeModelApiResp.getCode() != 200) {
            return "Error calling Zhipu AI: " + invokeModelApiResp.getMsg();
        }
        
        try {
            // The response structure is complex, we simplify it here to just get the content text
            // Typically: data -> choices -> index 0 -> message -> content
            // The SDK might return a mapped object or a raw one.
            // ModelApiResponse.getData().getChoices().get(0).getMessage().getContent()
            // However, to avoid import errors if method names changed, we can serialize/inspect or use safe getters.
            // Let's assume standard structure:
            return invokeModelApiResp.getData().getChoices().get(0).getMessage().getContent().toString();
        } catch (Exception e) {
            return "Error parsing AI response: " + e.getMessage();
        }
    }
}
