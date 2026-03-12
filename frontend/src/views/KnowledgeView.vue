<template>
  <div class="knowledge-view">
    <div class="page-header">
      <h1>📚 知识库管理</h1>
      <p class="subtitle">管理客服知识库内容</p>
    </div>

    <div class="content-placeholder">
      <div class="placeholder-icon">📚</div>
      <h2>功能开发中...</h2>
      <p>知识库管理功能正在紧张开发中，敬请期待！</p>
      <p class="coming-soon">预计功能：知识条目管理、批量导入导出、分类标签等</p>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.knowledge-view {
  padding: 30px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  color: #333;
  margin: 0 0 10px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.content-placeholder {
  background: white;
  padding: 60px 20px;
  border-radius: 15px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.content-placeholder h2 {
  color: #333;
  margin-bottom: 10px;
}

.content-placeholder p {
  color: #666;
}

.coming-soon {
  margin-top: 20px;
  font-size: 14px;
  color: #999;
}
</style>
