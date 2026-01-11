<template>
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-section">
          <div class="footer-brand">
            <span class="logo-icon">🚩</span>
            <span class="brand-name">RedSeeker</span>
          </div>
          <p class="footer-description">
            个性化红色旅游系统，传承红色基因，弘扬革命精神
          </p>
          <div class="social-links">
            <a href="#" class="social-link">📘</a>
            <a href="#" class="social-link">🐦</a>
            <a href="#" class="social-link">📷</a>
          </div>
        </div>
  
        <div class="footer-section">
          <h3>快速导航</h3>
          <ul class="footer-links">
            <li><router-link to="/">首页</router-link></li>
            <li><router-link to="/recommend">旅游推荐</router-link></li>
            <li><router-link to="/route">路线规划</router-link></li>
            <li><router-link to="/places">场所查询</router-link></li>
            <li><router-link to="/diary">旅游日记</router-link></li>
          </ul>
        </div>
  
        <div class="footer-section">
          <h3>联系我们</h3>
          <ul class="contact-info">
            <li>📧 contact@redseeker.com</li>
            <li>📞 400-123-4567</li>
            <li>📍 北京市海淀区</li>
          </ul>
        </div>
  
        <div class="footer-section">
          <h3>系统状态</h3>
          <div class="system-status">
            <div class="status-item">
              <span class="status-indicator online"></span>
              <span>在线用户: 156</span>
            </div>
            <div class="status-item">
              <span class="status-indicator online"></span>
              <span>服务状态: 正常</span>
            </div>
            <div class="status-item">
              <span class="status-indicator"></span>
              <span>已规划路线: 2,347</span>
            </div>
          </div>
        </div>
      </div>
  
      <div class="footer-bottom">
        <p>© 2024 RedSeeker 个性化旅游系统. 保留所有权利. | 数据结构课程设计项目</p>
      </div>
    </footer>
  </template>
  
  <script setup>
  // 页脚组件，不需要复杂逻辑
  </script>
  
  <style scoped>
  .footer {
    background: linear-gradient(135deg, #2c3e50 0%, #1a2530 100%);
    color: #e2e8f0;
    padding: 40px 0 20px;
    margin-top: 60px;
  }
  
  .footer-content {
    max-width: 1200px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 40px;
    padding: 0 20px;
  }
  
  .footer-section h3 {
    color: white;
    margin-bottom: 20px;
    font-size: 18px;
    position: relative;
    padding-bottom: 10px;
  }
  
  .footer-section h3::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 40px;
    height: 3px;
    background: linear-gradient(90deg, #ff7e5f, #feb47b);
  }
  
  .footer-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 15px;
  }
  
  .logo-icon {
    font-size: 24px;
  }
  
  .brand-name {
    font-size: 20px;
    font-weight: bold;
    color: white;
  }
  
  .footer-description {
    line-height: 1.6;
    margin-bottom: 20px;
    opacity: 0.9;
  }
  
  .social-links {
    display: flex;
    gap: 15px;
  }
  
  .social-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 50%;
    color: white;
    text-decoration: none;
    transition: all 0.3s ease;
  }
  
  .social-link:hover {
    background: linear-gradient(135deg, #ff7e5f, #feb47b);
    transform: translateY(-3px);
  }
  
  .footer-links, .contact-info {
    list-style: none;
    padding: 0;
  }
  
  .footer-links li, .contact-info li {
    margin-bottom: 10px;
  }
  
  .footer-links a, .contact-info li {
    color: #cbd5e1;
    text-decoration: none;
    transition: color 0.3s ease;
  }
  
  .footer-links a:hover {
    color: #ff7e5f;
    padding-left: 5px;
  }
  
  .status-item {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
  }
  
  .status-indicator {
    width: 8px;
    height: 8px;
    background: #4ade80;
    border-radius: 50%;
  }
  
  .status-indicator.online {
    background: #4ade80;
    box-shadow: 0 0 10px #4ade80;
  }
  
  .footer-bottom {
    max-width: 1200px;
    margin: 40px auto 0;
    padding: 20px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    text-align: center;
    font-size: 14px;
    color: #94a3b8;
  }
  </style>