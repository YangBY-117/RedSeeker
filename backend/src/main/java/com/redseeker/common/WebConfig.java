package com.redseeker.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebConfig.class);

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 使用当前工作目录（通常是backend目录）下的uploads
    Path uploads = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
    
    // 如果backend目录下没有，尝试项目根目录
    if (!Files.exists(uploads) && !Files.exists(uploads.getParent())) {
      Path parent = uploads.getParent();
      if (parent != null && parent.getFileName().toString().equals("backend")) {
        uploads = parent.getParent().resolve("uploads").toAbsolutePath().normalize();
      }
    }
    
    // 直接构建file:///格式的路径，避免URI转换问题
    String absolutePath = uploads.toString().replace("\\", "/");
    // Windows路径：C:/... -> file:///C:/...
    String finalLocation = "file:///" + absolutePath;
    if (!finalLocation.endsWith("/")) {
      finalLocation = finalLocation + "/";
    }
    
    LOGGER.info("配置静态资源服务: /uploads/** -> {}", finalLocation);
    LOGGER.info("上传目录绝对路径: {}", uploads);
    
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations(finalLocation)
        .setCachePeriod(3600); // 缓存1小时
    
    // 确保上传目录存在
    try {
      Path avatarsDir = uploads.resolve("avatars");
      Path diariesDir = uploads.resolve("diaries");
      Files.createDirectories(avatarsDir);
      Files.createDirectories(diariesDir);
      LOGGER.info("上传目录已创建: avatars={}, diaries={}", avatarsDir, diariesDir);
      
      // 验证目录是否真的存在
      if (Files.exists(avatarsDir)) {
        LOGGER.info("头像目录验证成功: {}", avatarsDir);
      } else {
        LOGGER.warn("头像目录创建失败: {}", avatarsDir);
      }
      if (Files.exists(diariesDir)) {
        LOGGER.info("日记目录验证成功: {}", diariesDir);
      } else {
        LOGGER.warn("日记目录创建失败: {}", diariesDir);
      }
    } catch (Exception ex) {
      LOGGER.error("创建上传目录失败", ex);
    }
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/uploads/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "HEAD", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);
  }
}
