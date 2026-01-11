<template>
    <div class="layout">
      <Navbar />
      <main class="main-content">
        <slot />
      </main>
      <Footer />
    </div>
  </template>
  
  <script setup>
  import Navbar from './Navbar.vue'
  import Footer from './Footer.vue'
  </script>
  
  <style scoped>
  .layout {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }
  
  .main-content {
    flex: 1;
    max-width: 1200px;
    width: 100%;
    margin: 0 auto;
    padding: 20px;
  }
  </style>