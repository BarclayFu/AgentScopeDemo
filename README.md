# 智能客服系统 (AgentScopeDemo)

基于 AgentScope Java 框架构建的智能客服系统，提供意图识别、多轮对话管理和知识库问答功能。前端使用 Vue.js 构建。

## 功能特性

### 核心功能
- **智能对话**: 基于大语言模型的自然语言处理
- **意图识别**: 自动识别用户意图（查询订单、办理退款、查询物流等）
- **多轮对话**: 维护上下文的多轮对话能力
- **知识库问答**: 基于 RAG 技术的知识库检索
- **流式输出**: SSE 实现打字机效果的实时响应

### 业务功能
- 订单状态查询
- 物流信息查询
- 退款处理
- 产品信息查询
- 知识库管理

## 技术架构

### 后端
- **框架**: Spring Boot 3.1.5
- **AI框架**: AgentScope Java 1.0.9
- **语言**: Java 17
- **构建工具**: Maven

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **状态管理**: Pinia
- **路由**: Vue Router

### 外部服务
- **LLM**: 阿里云百炼 (DashScope) / OpenAI 兼容 API
- **向量数据库**: Milvus
- **嵌入模型**: BAAI/bge-m3

## 项目结构

```
AgentScopeDemo/
├── customer-service-agent/     # 后端项目 (Spring Boot)
│   ├── src/main/java/
│   │   └── com/example/customerservice/
│   │       ├── agent/         # Agent配置
│   │       ├── controller/    # REST控制器
│   │       ├── service/       # 业务服务
│   │       ├── tools/         # 工具类
│   │       └── config/        # 配置类
│   └── src/main/resources/
│       └── application.yml    # 应用配置
│
├── frontend/                  # 前端项目 (Vue 3)
│   ├── src/
│   │   ├── api/              # API封装
│   │   ├── router/           # 路由配置
│   │   ├── stores/           # Pinia状态管理
│   │   ├── views/            # 页面视图
│   │   ├── layouts/          # 布局组件
│   │   └── components/       # 通用组件
│   ├── .env.development      # 开发环境配置
│   └── vite.config.js       # Vite配置
│
└── openspec/                 # 变更提案规范
    ├── project.md            # 项目规范
    ├── specs/                # 规范文档
    └── changes/              # 变更提案
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- Milvus 向量数据库（可选，用于知识库）

### 后端设置

1. 配置环境变量:
   ```bash
   export CHAT_API_KEY=your_api_key
   export CHAT_BASE_URL=your_api_base_url
   export EMBEDDING_API_KEY=your_embedding_key
   export EMBEDDING_BASE_URL=your_embedding_url
   ```

2. 构建并运行:
   ```bash
   cd customer-service-agent
   mvn clean package
   java -jar target/customer-service-agent-1.0.0.jar
   ```

3. 访问后端: http://localhost:8080

### 前端设置

1. 安装依赖:
   ```bash
   cd frontend
   npm install
   ```

2. 开发模式:
   ```bash
   npm run dev
   ```
   前端运行在 http://localhost:5173，自动代理 `/api` 到后端

3. 生产构建:
   ```bash
   npm run build
   ```
   构建产物在 `dist/` 目录

## API 接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/chat/message | 发送消息（非流式） |
| GET | /api/chat/stream | SSE 流式响应 |
| GET | /api/chat/sessions/count | 活跃会话数 |
| DELETE | /api/chat/session/{userId} | 清除会话 |
| GET | /api/chat/health | 健康检查 |
| GET | /api/monitoring/stats | 监控统计 |

## 扩展开发

### 添加新工具

1. 在 `customer-service-agent/.../tools/` 中创建新的工具类
2. 使用 `@Tool` 注解标记工具方法
3. 在 Agent 配置中注册工具

### 添加新页面

1. 在 `frontend/src/views/` 创建 Vue 组件
2. 在 `frontend/src/router/index.js` 添加路由
3. 在 `frontend/src/layouts/AppLayout.vue` 添加导航

## 许可证

MIT License
