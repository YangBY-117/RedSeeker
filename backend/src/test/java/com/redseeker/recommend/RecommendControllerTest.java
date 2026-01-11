package com.redseeker.recommend;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class RecommendControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AiService aiService;

  @Test
  public void testGetRecommendations_ContentBased() throws Exception {
    RecommendRequest request = new RecommendRequest();
    request.setCity("Shanghai");
    // "革命旧址" corresponds to item 1
    request.setPreferences(List.of("革命旧址"));

    mockMvc.perform(post("/api/recommend/list")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        // Item 1 should have high score due to content match
        .andExpect(jsonPath("$.data[0].category").value("革命旧址"));
  }

  @Test
  public void testGetRecommendations_CollaborativeFiltering() throws Exception {
    RecommendRequest request = new RecommendRequest();
    request.setCity("Shanghai");
    // User 101 prefers Shanghai sites (Item 1, 2). User 102 prefers Memorials (Item 3, 5).
    // Let's query as User 102, who hasn't rated "遵义会议会址" (Item 4).
    // Or let's just check that we get a response.
    request.setUserId(102L); 

    mockMvc.perform(post("/api/recommend/list")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  public void testAiPlan() throws Exception {
    // Mock the AI service
    when(aiService.generateContent(anyString())).thenReturn("这是AI生成的Mock行程方案。");

    AiPlanRequest request = new AiPlanRequest();
    request.setPrompt("我想去上海看红色的景点");
    request.setDays(2);
    request.setCity("Shanghai");

    mockMvc.perform(post("/api/recommend/ai-plan")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.plans").isArray())
        .andExpect(jsonPath("$.data.plans[0].description").value("这是AI生成的Mock行程方案。"));
  }
}
