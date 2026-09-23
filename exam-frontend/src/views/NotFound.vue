<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="text">页面不存在或已被移除</p>
    <el-button type="primary" @click="$router.replace('/')">返回首页</el-button>
  </div>
</template>

<script setup></script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #f5f7fa;
}

.code {
  font-size: 96px;
  font-weight: 700;
  color: #c0c4cc;
  line-height: 1;
}

.text {
  color: #909399;
  font-size: 16px;
}
</style>
