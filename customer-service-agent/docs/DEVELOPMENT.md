# 后端开发文档

本文档面向继续维护 `customer-service-agent` 的开发者，重点说明当前实现结构，而不是历史方案。

## 核心模块

### Agent 运行链路

- `CustomerServiceAgentConfig`
  - 负责组装 Agent、模型和工具
- `ChatSessionService`
  - 负责按用户管理会话
  - 提供普通聊天和流式聊天能力
  - 在返回前会过滤 `think` / `thought` / `reasoning` 内容
- `CustomerServiceTools`
  - 提供订单、物流、退款、产品信息、知识录入等工具

### 管理后台 API

- `MonitoringController`
  - 仪表盘使用的监控接口
- `KnowledgeController`
  - 知识库管理台使用的接口
- `WebCorsConfig`
  - 开发环境全局跨域配置

### 知识库链路

- `RagConfig`
  - 创建 `EmbeddingModel`
  - 创建 `Knowledge`
  - 底层使用 `MilvusStore`
- `KnowledgeBaseService`
  - 管理知识条目注册表
  - 执行索引建立、删除和重建
  - 对外提供检索、列表、状态等服务

## 请求流转

### 聊天

```text
前端 ChatView
-> ChatController
-> ChatSessionService
-> AgentScope Agent
-> CustomerServiceTools / KnowledgeBaseService
-> 返回普通响应或 SSE 流
```

### 仪表盘

```text
前端 DashboardView
-> MonitoringController
-> AgentMonitoringService
-> DTO 响应
```

### 知识库管理

```text
前端 KnowledgeView
-> KnowledgeController
-> KnowledgeBaseService
-> knowledge-entries.json + Milvus
```

## 知识库实现细节

### 条目来源

知识条目有三种来源：

1. 应用初始化时写入的默认 seed 数据
2. 管理台手工新增的条目
3. Agent 工具 `add_knowledge` 新增的条目

### 初始化流程

`KnowledgeBaseService.init()` 在应用启动后执行：

1. 先读取 `data/knowledge-entries.json`
2. 如果为空，则写入默认 seed 数据
3. 检查每个条目是否已有 `chunkIds`
4. 缺失则重新建立向量索引

### 索引建立

当前索引流程不是手写倒排索引，而是向量化写入 Milvus：

1. 读入条目原始文本
2. 用 `TextReader` 切块
3. 为每个 chunk 生成 `chunkId`
4. 写入 `title/source/type/entryId` 等 payload
5. 调用 `knowledgeBase.addDocuments(...)`

检索时通过 `knowledgeBase.retrieve(...)` 获取结果。

### 为什么页面能显示文本预览

管理台展示的 `contentPreview` 来自知识条目注册表本身，不是从向量库反查。

也就是说：

- 页面列表数据来源：`data/knowledge-entries.json`
- 检索命中来源：Milvus 向量库

## 本地开发

### 启动

```bash
mvn spring-boot:run
```

### 测试

```bash
mvn test
```

### 常见排查

- 前端请求失败：
  - 先确认后端是否已重启
  - 再确认 `WebCorsConfig` 是否生效
- 知识库页面有条目但检索不到：
  - 检查 Milvus 是否正常
  - 检查 embedding 配置
  - 尝试调用知识库重建接口
- 界面出现 `Think:` 等内容：
  - 优先检查 `ChatSessionService` 的输出清洗是否被新改动绕过

## 后续开发建议

- 新能力优先走 OpenSpec 变更流程
- 知识库下一阶段建议做文件上传和批量导入
- 仪表盘下一阶段建议增加更细粒度的趋势数据和错误明细
