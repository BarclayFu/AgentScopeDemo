# 项目交接说明

这份文档用于在新电脑或新环境下快速恢复上下文，避免只靠聊天记录继续开发。

## 当前已完成内容

### 前端

- 聊天页已接入流式 SSE 聊天
- 支持多用户切换
- 聊天记录会持久化到浏览器 `localStorage`
- 设置页可保存默认用户、流式速度、API 地址
- 仪表盘已接入真实监控接口
- 知识库管理页已支持：
  - 列表查看
  - 手工新增文本知识
  - 删除知识条目
  - 重建索引
  - 查看知识库状态

### 后端

- `ChatController` 支持普通消息和流式消息
- `MonitoringController` 已返回结构化 DTO
- `KnowledgeController` 已提供知识库管理接口
- 已补统一跨域配置 [WebCorsConfig.java](../customer-service-agent/src/main/java/com/example/customerservice/config/WebCorsConfig.java)
- 响应输出已增加对 `think` / `thought` / `reasoning` 内容的过滤，避免展示到客户界面

## OpenSpec 状态

当前主要变更：

- `add-vue-frontend`
- `add-admin-dashboard-mvp`
- `add-knowledge-base-console-mvp`

说明：

- 这些 change 已经编写并通过过本地 `openspec validate`
- 如果下一步继续做新能力，建议新开 proposal，不要直接在旧 change 上无限追加

## 知识库实现说明

知识库当前是两层数据：

1. Milvus 中的向量数据
2. 本地注册表 `customer-service-agent/data/knowledge-entries.json`

其中：

- 管理台页面展示的知识条目来自本地注册表
- 检索时调用的是 AgentScope `Knowledge.retrieve(...)`
- 新增或重建时，会把文本切块、生成 embedding，并写入 Milvus
- 每个条目会记录自己的 `chunkId`，用于删除和重建

关键代码：

- [KnowledgeBaseService.java](../customer-service-agent/src/main/java/com/example/customerservice/service/KnowledgeBaseService.java)
- [RagConfig.java](../customer-service-agent/src/main/java/com/example/customerservice/config/RagConfig.java)
- [KnowledgeController.java](../customer-service-agent/src/main/java/com/example/customerservice/controller/KnowledgeController.java)

## 启动顺序

### 后端

1. 启动 Milvus
2. 配置环境变量：

```bash
export CHAT_API_KEY=your_chat_api_key
export CHAT_BASE_URL=your_chat_base_url
export EMBEDDING_API_KEY=your_embedding_api_key
export EMBEDDING_BASE_URL=your_embedding_base_url
```

3. 启动 Spring Boot：

```bash
cd customer-service-agent
mvn spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认开发地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`

## 本地持久化说明

前端会用到两个本地存储键：

- `agentscope.admin.settings`
- `agentscope.chat.state`

如果页面行为异常，尤其是 API 地址不对、切换用户异常、刷新后状态奇怪，可以先清掉这两个键再试。

## 已知问题与注意事项

- 聊天页宽度样式还可以继续优化，目前属于可用但未最终定稿
- Java 测试在当前这台机器上没有完整跑过，因为之前环境里缺少 `mvn`
- `customer-service-agent/data/knowledge-entries.json` 是运行后生成或更新的数据文件，换环境时如果不想带旧数据，可以删除后重新初始化

## 推荐下一步

建议优先做下面两项之一：

1. 把 Java 本地构建和测试链路完整跑通
2. 新开 OpenSpec 变更做“知识库导入增强”，把手工文本录入升级成文件上传或批量导入

如果继续迭代，优先从这里入手：

- [frontend/src/views/KnowledgeView.vue](../frontend/src/views/KnowledgeView.vue)
- [customer-service-agent/src/main/java/com/example/customerservice/service/KnowledgeBaseService.java](../customer-service-agent/src/main/java/com/example/customerservice/service/KnowledgeBaseService.java)
- [openspec/changes/add-knowledge-base-console-mvp](../openspec/changes/add-knowledge-base-console-mvp)
