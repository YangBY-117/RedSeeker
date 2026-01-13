package com.redseeker.recommend;

public interface AiService {
    /**
     * Generate content using AI model
     * @param prompt prompt text
     * @return AI generated content
     */
    String generateContent(String prompt);
}
