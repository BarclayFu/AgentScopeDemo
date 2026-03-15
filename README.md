# AgentScopeDemo

基于 AgentScope Java 和 Vue 3 的智能客服系统 Demo，当前已经具备可运行的聊天前端、管理后台 MVP，以及基础知识库管理能力。

## 当前状态

- 聊天主链路可用，支持普通消息和 SSE 流式输出
- 前端管理后台已包含仪表盘、设置页、知识库管理页
- 后端提供监控接口、知识库管理接口和全局 CORS 配置
- 知识库使用 AgentScope RAG + Milvus，管理台维护一份本地知识条目注册表
- 项目开发过程遵循 OpenSpec，现有阶段性变更都记录在 `openspec/changes/`

建议第一次接手时按这个顺序阅读：

1. [交接说明](docs/HANDOFF.md)
2. [前端说明](frontend/README.md)
3. [后端说明](customer-service-agent/README.md)
4. [OpenSpec 指南](openspec/AGENTS.md)

## 仓库结构

```text
AgentScopeDemo/
├── customer-service-agent/      Spring Boot 后端
├── frontend/                    Vue 3 管理前端
├── openspec/                    需求与变更规范
└── docs/                        交接与补充文档
```

## 快速启动

### 1. 启动后端

环境要求：

- JDK 17+
- Maven 3.6+
- Milvus
- 可用的聊天模型和 embedding 模型接口

示例环境变量：

```bash
export CHAT_API_KEY=your_chat_api_key
export CHAT_BASE_URL=your_chat_base_url
export EMBEDDING_API_KEY=your_embedding_api_key
export EMBEDDING_BASE_URL=your_embedding_base_url
```

启动命令：

```bash
cd customer-service-agent
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

### 2. 启动前端

环境要求：

- Node.js 18+

启动命令：

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

## 已实现的页面

- `/`：聊天页
- `/dashboard`：管理后台仪表盘
- `/knowledge`：知识库管理台 MVP
- `/settings`：前端运行时设置页

## 主要接口

### 聊天接口

- `POST /api/chat/message`
- `GET /api/chat/stream`
- `GET /api/chat/sessions/count`
- `DELETE /api/chat/session/{userId}`
- `GET /api/chat/health`

### 监控接口

- `GET /api/monitoring/stats`
- `GET /api/monitoring/status`
- `POST /api/monitoring/reset`

### 知识库接口

- `GET /api/knowledge/entries`
- `POST /api/knowledge/entries`
- `DELETE /api/knowledge/entries/{entryId}`
- `POST /api/knowledge/rebuild`
- `GET /api/knowledge/status`

## OpenSpec 相关

当前已经存在的主要变更：

- `add-vue-frontend`
- `add-admin-dashboard-mvp`
- `add-knowledge-base-console-mvp`

继续新增功能前，先阅读 [openspec/AGENTS.md](openspec/AGENTS.md) 并按流程创建新 change proposal。
