package com.redseeker.recommend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class RecommendControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void testGetRecommendations() throws Exception {
    RecommendRequest request = new RecommendRequest();
    request.setCity("Shanghai");
    request.setPreferences(List.of("革命旧址"));

    mockMvc.perform(post("/api/recommend/list")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        // Should contain specific mock data we added like "中国共产党第一次全国代表大会会址" which is "革命旧址"
        .andExpect(jsonPath("$.data[0].category").value("革命旧址"));
  }

  @Test
  public void testAiPlan() throws Exception {
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
        .andExpect(jsonPath("$.data.plans[0].title").exists());
  }
}
