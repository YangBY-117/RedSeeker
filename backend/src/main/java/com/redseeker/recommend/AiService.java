package com.redseeker.recommend;

public interface AiService {
    /**
     * 调用AI模型生成内容
     * @param prompt 提示词
     * @return AI生成的内容
     */
    String generateContent(String prompt);
}
