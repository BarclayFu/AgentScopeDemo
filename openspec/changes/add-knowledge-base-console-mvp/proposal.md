# 变更：交付知识库管理台 MVP

## 为什么
当前系统已经具备知识库检索能力，但知识库内容仍然完全由后端初始化逻辑和工具链维护：
- 前端 `KnowledgeView` 仍然是占位页
- 后端缺少知识库列表、上传、删除、重建索引等管理接口
- 运营或开发人员无法从界面上确认当前知识库状态，也无法主动维护内容

这会让知识库成为“系统里存在，但无法运营”的能力，难以支持后续客服效果优化。

## 什么变化
- 新增知识库管理台 MVP 页面，替换现有占位页
- 为知识库增加结构化管理接口，用于列表、添加、删除、刷新索引和查看状态
- 在前端展示知识库条目数量、最近更新时间、操作结果和刷新状态
- 补充最小测试和文档说明，确保这套能力可以稳定迭代

## 不在本次范围内
- 不实现富文本知识编辑器
- 不实现复杂的标签、分类、权限体系
- 不接入外部对象存储或完整文件上传中心
- 不重构现有 RAG 主链路

## 影响
- 受影响的规范：`knowledge-console`
- 受影响的代码：
  - 前端：`frontend/src/views/KnowledgeView.vue`
  - 前端：`frontend/src/api/index.js`
  - 前端：`frontend/src/stores/`
  - 后端：`customer-service-agent/src/main/java/com/example/customerservice/service/KnowledgeBaseService.java`
  - 后端：`customer-service-agent/src/main/java/com/example/customerservice/controller/`
  - 后端：`customer-service-agent/src/main/java/com/example/customerservice/tools/KnowledgeBaseTools.java`

## 约束与依赖
- 本变更建立在现有 Vue 前端和管理后台 MVP 的基础上
- MVP 阶段优先支持文本知识条目管理，不要求一次性支持所有文档格式
- 新增的管理接口不能影响现有知识库检索接口和聊天问答流程
