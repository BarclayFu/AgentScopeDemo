# 前端项目说明

这是智能客服系统的 Vue 3 前端，负责聊天页面和管理后台界面。

## 已完成能力

- 聊天页：支持 SSE 流式输出和多用户切换
- 仪表盘：展示结构化监控摘要、健康状态和最近检查时间
- 设置页：支持保存默认用户、流式速度和 API 地址，并写入 localStorage

## 常用命令

```bash
npm install
npm run dev
npm run build
npm run test
```

## 运行时设置

设置页会把前端操作偏好保存到浏览器 localStorage，键名为 `agentscope.admin.settings`。

包含以下字段：

- `apiBaseUrl`：API 根地址，可留空以走 Vite 代理或同源部署
- `defaultUserId`：聊天页默认用户 ID
- `defaultStreamInterval`：聊天页默认流式速度

如果 `apiBaseUrl` 已经带有 `/api`，前端会自动兼容，不会重复拼接。

## 页面说明

- `/`：在线客服
- `/dashboard`：管理后台仪表盘
- `/settings`：前端设置页
- `/knowledge`：知识库管理占位页，后续阶段实现
