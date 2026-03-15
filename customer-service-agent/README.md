# customer-service-agent

Spring Boot 后端服务，负责 AgentScope 智能体运行、会话管理、监控统计和知识库管理接口。

## 当前能力

- 普通聊天接口和 SSE 流式聊天接口
- 多用户会话管理
- 订单查询、物流查询、退款处理、产品信息查询等工具调用
- 监控接口：
  - 活跃会话数
  - 消息数
  - 工具调用数
  - 错误数
  - 平均响应时间
- 知识库管理接口：
  - 条目列表
  - 新增条目
  - 删除条目
  - 重建索引
  - 状态查看
- 全局 CORS 配置，适配本地前端开发

## 目录结构

```text
customer-service-agent/
├── src/main/java/com/example/customerservice/
│   ├── agent/         Agent 配置
│   ├── config/        Spring 配置、RAG 配置、CORS 配置
│   ├── controller/    REST API 控制器
│   ├── dto/           接口请求/响应 DTO
│   ├── service/       核心业务服务
│   └── tools/         Agent 工具定义
├── src/main/resources/application.yml
├── data/              知识条目注册表
└── pom.xml
```

## 环境要求

- JDK 17+
- Maven 3.6+
- Milvus
- 可用的聊天模型与 embedding 模型接口

## 配置说明

主要配置在 [application.yml](src/main/resources/application.yml)。

需要的环境变量：

```bash
export CHAT_API_KEY=your_chat_api_key
export CHAT_BASE_URL=your_chat_base_url
export EMBEDDING_API_KEY=your_embedding_api_key
export EMBEDDING_BASE_URL=your_embedding_base_url
```

默认端口：

- 应用：`8080`
- Milvus：`19530`

## 运行方式

```bash
cd customer-service-agent
mvn spring-boot:run
```

或：

```bash
mvn clean package
java -jar target/customer-service-agent-1.0.0.jar
```

## 关键接口

### 聊天

- `POST /api/chat/message`
- `GET /api/chat/stream`
- `GET /api/chat/sessions/count`
- `DELETE /api/chat/session/{userId}`
- `GET /api/chat/health`

### 监控

- `GET /api/monitoring/stats`
- `GET /api/monitoring/status`
- `POST /api/monitoring/reset`

### 知识库

- `GET /api/knowledge/entries`
- `POST /api/knowledge/entries`
- `DELETE /api/knowledge/entries/{entryId}`
- `POST /api/knowledge/rebuild`
- `GET /api/knowledge/status`

## 知识库数据流

当前知识库是“注册表 + 向量库”的双层结构：

1. 管理条目保存在 `data/knowledge-entries.json`
2. 实际检索写入 Milvus collection

建立索引时的流程：

1. 接收文本条目
2. 使用 `TextReader` 切块
3. 生成 `DocumentMetadata`
4. 调用 `knowledgeBase.addDocuments(...)`
5. 通过 AgentScope 的 `SimpleKnowledge + MilvusStore` 写入向量库

关键代码：

- [KnowledgeBaseService.java](src/main/java/com/example/customerservice/service/KnowledgeBaseService.java)
- [RagConfig.java](src/main/java/com/example/customerservice/config/RagConfig.java)

## 开发提示

- 跨域规则集中在 [WebCorsConfig.java](src/main/java/com/example/customerservice/config/WebCorsConfig.java)
- 若前端仍报接口异常，先确认后端是否已重启
- `think` / `reasoning` 内容已经在服务端做过过滤，不应直接显示给客户
- 管理台看到的知识内容预览来自本地注册表，不是从 Milvus 反查出来的

## 测试

```bash
mvn test
```

目前仓库中已经包含控制器层测试，例如：

- `MonitoringControllerTest`
- `KnowledgeControllerTest`
