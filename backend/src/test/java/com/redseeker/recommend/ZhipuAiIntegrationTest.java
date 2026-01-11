package com.redseeker.recommend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ZhipuAiIntegrationTest {

    @Autowired
    private AiService aiService;

    @Test
    public void testRealAiCall() {
        System.out.println("Beginning Real AI API Test...");
        String prompt = "Testing connection. Please reply with 'pong'.";
        String response = aiService.generateContent(prompt);
        
        System.out.println("--------------------------------------------------");
        System.out.println("Real AI Response: " + response);
        System.out.println("--------------------------------------------------");
        
        // Basic validation
        assertThat(response).isNotNull();
        assertThat(response).doesNotStartWith("Error: API Key not configured");
    }
}
