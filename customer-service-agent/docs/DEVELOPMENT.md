# 开发文档

## 项目架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    客户端 (Web/移动应用)                     │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTP/REST API
┌─────────────────────▼───────────────────────────────────────┐
│                    Spring Boot 应用                          │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                  REST 控制器 (ChatController)           ││
│  └─────────────────────┬───────────────────────────────────┘│
│                        │
│  ┌─────────────────────▼───────────────────────────────────┐│
│  │               会话服务 (ChatSessionService)             ││
│  │  - 管理用户会话状态                                     ││
│  │  - 创建和维护Agent实例                                  ││
│  └─────────────────────┬───────────────────────────────────┘│
│                        │
│  ┌─────────────────────▼───────────────────────────────────┐│
│  │              智能客服Agent (ReActAgent)                  ││
│  │  - 基于AgentScope框架                                   ││
│  │  - 使用DashScope模型                                    ││
│  │  - 调用工具处理用户请求                                 ││
│  └─────────────────────┬───────────────────────────────────┘│
│                        │
│  ┌─────────────────────▼───────────────────────────────────┐│
│  │                    工具系统                             ││
│  │  ┌────────────────────────────────────────────────────┐ ││
│  │  │           客服工具 (CustomerServiceTools)          │ ││
│  │  │  - query_order_status: 查询订单状态                │ ││
│  │  │  - process_refund: 办理退款                        │ ││
│  │  │  - query_product_info: 查询产品信息                │ ││
│  │  │  - query_shipping_status: 查询物流状态             │ ││
│  │  └────────────────────────────────────────────────────┘ ││
│  │  ┌────────────────────────────────────────────────────┐ ││
│  │  │           知识库工具 (KnowledgeBaseTools)          │ ││
│  │  │  - search_knowledge_base: 检索知识库               │ ││
│  │  └────────────────────────────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│                        │
│  ┌─────────────────────▼───────────────────────────────────┐│
│  │                 知识库系统 (RAG)                        ││
│  │  - SimpleKnowledge实现                                  ││
│  │  - 文本嵌入模型                                         ││
│  │  - 文档检索功能                                         ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 核心组件说明

1. **主应用类** (`CustomerServiceAgentApplication`)
   - Spring Boot应用入口点
   - 负责初始化应用上下文

2. **REST控制器** (`ChatController`)
   - 提供HTTP API接口
   - 处理用户消息、会话管理等请求

3. **会话服务** (`ChatSessionService`)
   - 管理用户会话状态
   - 为每个用户创建独立的Agent实例

4. **Agent配置** (`CustomerServiceAgentConfig`)
   - 配置客服Agent及其依赖组件
   - 初始化模型、工具包等

5. **工具系统**
   - **客服工具**: 处理具体业务逻辑
   - **知识库工具**: 提供知识检索能力

## 代码结构

```
src/
├── main/
│   ├── java/com/example/customerservice/
│   │   ├── CustomerServiceAgentApplication.java    # 主应用类
│   │   ├── agent/                                  # Agent配置
│   │   │   └── CustomerServiceAgentConfig.java
│   │   ├── controller/                             # REST控制器
│   │   │   └── ChatController.java
│   │   ├── service/                                # 业务服务
│   │   │   ├── ChatSessionService.java
│   │   │   └── KnowledgeBaseService.java
│   │   └── tools/                                  # 工具类
│   │       ├── CustomerServiceTools.java
│   │       └── KnowledgeBaseTools.java
│   └── resources/
│       ├── application.yml                         # 应用配置
│       └── static/                                 # 静态资源
│           └── index.html                          # 测试页面
└── test/
    └── java/com/example/customerservice/
        └── CustomerServiceAgentApplicationTests.java
```

## 扩展开发指南

### 添加新工具

1. 在 `CustomerServiceTools.java` 中添加新的工具方法:

```java
@Tool(name = "new_tool_name", description = "工具功能描述")
public static String newToolMethod(
        @ToolParam(name = "parameterName", description = "参数描述") String parameter) {
    // 实现工具逻辑
    return "工具执行结果";
}
```

2. 在Agent的系统提示词中添加使用说明:

```java
// 在CustomerServiceAgentConfig.java中更新系统提示词
"- 若客户需要[某种功能]，请使用new_tool_name工具"
```

### 扩展知识库

1. 在 `KnowledgeBaseTools.java` 的 `initializeDocuments()` 方法中添加新文档:

```java
String newContent = """
    新文档标题
    文档内容...
    """;
addDocumentToKnowledgeBase("新文档标题", newContent);
```

2. 或者实现外部文档导入功能（高级）:

```java
// 在KnowledgeBaseService中添加方法
public void importDocumentFromFile(String filePath) {
    // 实现文件读取和导入逻辑
}
```

### 自定义Agent行为

1. 修改 `CustomerServiceAgentConfig.java` 中的系统提示词
2. 调整Agent的行为规则和处理流程
3. 可以创建多个不同用途的Agent实例

## API接口文档

### 发送消息

```
POST /api/chat/message
Content-Type: application/json

{
  "userId": "用户ID",
  "message": "用户消息内容"
}

响应:
{
  "userId": "用户ID",
  "response": "Agent回复内容",
  "timestamp": 时间戳
}
```

### 查询活跃会话数

```
GET /api/chat/sessions/count

响应:
{
  "activeSessions": 会话数量
}
```

### 清除用户会话

```
DELETE /api/chat/session/{userId}

响应:
{
  "message": "用户会话已清除"
}
```

### 健康检查

```
GET /api/chat/health

响应:
{
  "status": "healthy",
  "service": "Customer Service Agent"
}
```

## 测试指南

### 单元测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=CustomerServiceAgentApplicationTests
```

### 集成测试

使用 `test-api.sh` 脚本进行API集成测试:

```bash
# 确保应用已启动
./start.sh

# 在另一个终端运行测试
./test-api.sh
```

### 性能测试

可以使用Apache Bench等工具进行性能测试:

```bash
ab -n 1000 -c 10 -p test-data.json -T "application/json" http://localhost:8080/api/chat/message
```

## 部署指南

### 环境要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本
- DashScope API Key

### 构建和部署

```bash
# 构建项目
mvn clean package

# 运行应用
java -jar target/customer-service-agent-1.0.0.jar

# 或使用Docker（需要Dockerfile）
docker build -t customer-service-agent .
docker run -p 8080:8080 customer-service-agent
```

### 配置管理

通过环境变量或配置文件管理配置:

```bash
export DASHSCOPE_API_KEY=your_api_key_here
export SERVER_PORT=8080
```

## 故障排除

### 常见问题

1. **API密钥错误**
   - 检查DASHSCOPE_API_KEY环境变量是否正确设置
   - 验证API密钥是否有效

2. **模型调用失败**
   - 检查网络连接
   - 确认DashScope服务状态

3. **知识库初始化失败**
   - 检查嵌入模型配置
   - 验证API密钥权限

### 日志查看

```bash
# 查看应用日志
tail -f logs/application.log

# 查看系统日志
journalctl -u customer-service-agent
```

## 性能优化建议

1. **会话管理优化**
   - 使用Redis等外部存储管理会话状态
   - 实现会话过期机制

2. **缓存策略**
   - 缓存常用查询结果
   - 使用本地缓存减少重复计算

3. **并发处理**
   - 调整线程池配置
   - 优化Agent实例管理

4. **数据库优化**
   - 为频繁查询字段添加索引
   - 优化查询语句
```
