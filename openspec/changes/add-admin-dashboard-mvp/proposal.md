# 变更：交付管理后台 MVP（仪表盘与设置）

## 为什么
现有 Vue 前端已经完成聊天主链路，但管理后台仍停留在占位阶段：
- 仪表盘页面只能展示静态占位内容，无法反映系统真实运行状态
- 设置页面的参数修改不会保存，也不会影响聊天页面行为
- 后端监控接口返回字符串，前端无法稳定消费结构化指标

如果继续叠加新页面，前端会看起来“功能很多”，但缺少可运营、可观测、可验证的后台基础能力。

## 什么变化
- 为管理后台增加可交付的仪表盘 MVP，展示真实监控指标和健康状态
- 将监控相关接口改为结构化 JSON 输出，便于前端稳定消费
- 为设置页面增加本地持久化能力，并让聊天页读取这些设置
- 为仪表盘和设置页补充基础加载态、错误态和测试覆盖

## 不在本次范围内
- 不接入 ARMS、Spring AI 或其他外部监控平台
- 不实现知识库管理页的上传、编辑和索引管理
- 不重构 AgentScope 主链路

## 影响
- 受影响的规范：`admin-console`
- 受影响的代码：
  - 前端：`frontend/src/views/DashboardView.vue`
  - 前端：`frontend/src/views/SettingsView.vue`
  - 前端：`frontend/src/views/ChatView.vue`
  - 前端：`frontend/src/api/index.js`
  - 前端：`frontend/src/stores/`
  - 后端：`customer-service-agent/src/main/java/com/example/customerservice/controller/MonitoringController.java`
  - 后端：`customer-service-agent/src/main/java/com/example/customerservice/service/AgentMonitoringService.java`

## 约束与依赖
- 本变更建立在 `add-vue-frontend` 已完成的基础上
- 新的监控接口必须与现有聊天接口兼容，不能影响当前聊天主流程
- 设置页只保存前端操作偏好，不承担密钥或服务端敏感配置管理
