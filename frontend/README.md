# frontend

Vue 3 管理前端，负责聊天页、仪表盘、设置页和知识库管理页。

## 当前页面

- `/`：聊天页
- `/dashboard`：监控仪表盘
- `/knowledge`：知识库管理台 MVP
- `/settings`：前端运行时设置页

## 已实现能力

- SSE 流式聊天
- 多用户切换
- 聊天记录本地持久化
- 仪表盘监控数据加载、自动刷新和手动刷新
- 设置页持久化
- 知识条目列表、新增、删除、重建索引

## 常用命令

```bash
npm install
npm run dev
npm run build
npm run test
```

开发默认地址：`http://localhost:5173`

## API 连接方式

开发环境支持两种方式：

1. `VITE_API_BASE_URL` 直接指向后端
2. 留空后走 Vite 代理 `/api -> http://localhost:8080`

默认配置见 [.env.development](.env.development) 和 [vite.config.js](vite.config.js)。

## 运行时设置

设置页会把前端偏好保存到浏览器 localStorage：

- `agentscope.admin.settings`

字段包括：

- `apiBaseUrl`
- `defaultUserId`
- `defaultStreamInterval`

聊天记录会保存在：

- `agentscope.chat.state`

如果切换环境后页面行为异常，可以先清掉这两个键再试。

## 开发说明

- 聊天页会优先读取设置页保存的默认用户和流式速度
- 如果后端地址里已经包含 `/api`，前端会自动兼容，避免重复拼接
- 仪表盘对监控接口做过兼容处理，可同时兼容旧字符串响应和新 JSON 响应
- 知识库页面展示的是后端返回的 `contentPreview`，不是前端自己截取

## 测试覆盖

当前已包含的前端测试主要包括：

- 运行时设置
- 仪表盘数据渲染
- 设置持久化
- 知识库管理页交互
