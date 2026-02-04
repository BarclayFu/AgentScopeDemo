# 智能客服Agent系统

基于AgentScope Java框架构建的智能客服系统，具备意图识别、多轮对话管理和知识库问答功能。

## 项目概述

本项目是一个企业级智能客服系统，利用AgentScope Java框架的强大能力，实现了以下核心功能：

1. **意图识别**：自动识别用户输入的意图，如查询订单、办理退款、查询物流等
2. **多轮对话管理**：维护用户对话状态，支持上下文关联的多轮对话
3. **知识库问答**：基于RAG技术，能够检索知识库中的信息回答用户问题
4. **工具调用**：通过工具调用实现具体业务操作，如查询订单状态、处理退款等

## 技术架构

- **后端框架**：Spring Boot 3.x
- **AI框架**：AgentScope Java
- **语言模型**：阿里云百炼平台(Qwen系列)
- **知识库**：基于AgentScope的RAG功能实现
- **构建工具**：Maven

## 功能特性

### 核心功能

1. **订单管理**
   - 查询订单状态和详情
   - 查询物流信息

2. **售后服务**
   - 处理退款请求
   - 查询产品信息

3. **知识库问答**
   - 常见问题解答
   - 产品使用指南
   - 售后服务政策

### 技术特性

1. **多用户会话管理**：为每个用户维护独立的对话上下文
2. **工具调用**：通过AgentScope的工具系统实现业务逻辑
3. **知识检索**：基于向量检索的知识库问答系统
4. **REST API**：提供标准的HTTP接口供前端调用

## 项目结构

```
customer-service-agent/
├── src/main/java/com/example/customerservice/
│   ├── CustomerServiceAgentApplication.java  # Spring Boot主应用类
│   ├── agent/
│   │   └── CustomerServiceAgentConfig.java   # Agent配置类
│   ├── controller/
│   │   └── ChatController.java               # REST控制器
│   ├── service/
│   │   ├── ChatSessionService.java           # 会话管理服务
│   │   └── KnowledgeBaseService.java         # 知识库服务(示例)
│   └── tools/
│       ├── CustomerServiceTools.java         # 客服工具类
│       └── KnowledgeBaseTools.java           # 知识库工具类
├── src/main/resources/
│   ├── application.yml                       # 应用配置文件
│   └── sessions/                             # 会话数据存储目录
├── pom.xml                                   # Maven配置文件
└── README.md                                 # 项目说明文档
```

## 快速开始

### 环境要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本
- DashScope API Key (阿里云百炼平台)

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd customer-service-agent
   ```

2. **配置API密钥**
   ```bash
   export DASHSCOPE_API_KEY=your_api_key_here
   ```

3. **构建项目**
   ```bash
   mvn clean package
   ```

4. **运行应用**
   ```bash
   mvn spring-boot:run
   ```

   或者
   ```bash
   java -jar target/customer-service-agent-1.0.0.jar
   ```

### API接口

应用启动后，默认在 `http://localhost:8080` 提供服务。

#### 发送消息
```
POST /api/chat/message
Content-Type: application/json

{
  "userId": "user123",
  "message": "我想查询订单ORD001的状态"
}
```

#### 查询活跃会话数
```
GET /api/chat/sessions/count
```

#### 清除用户会话(测试用)
```
DELETE /api/chat/session/{userId}
```

#### 健康检查
```
GET /api/chat/health
```

## 使用示例

### 查询订单状态
用户: "我想查询订单ORD001的状态"
系统: 返回订单详情，包括商品名称、价格、状态和下单日期

### 办理退款
用户: "我要为订单ORD002办理退款，商品有质量问题"
系统: 处理退款请求并返回退款编号和预计处理时间

### 查询物流信息
用户: "订单ORD003的物流状态如何？"
系统: 返回最新的物流状态和更新时间

### 知识库问答
用户: "如何联系人工客服？"
系统: 从知识库中检索相关信息并回答

## 扩展开发

### 添加新工具

1. 在 `CustomerServiceTools.java` 中添加新的工具方法
2. 使用 `@Tool` 注解标记工具方法
3. 在Agent的系统提示词中添加使用说明

### 扩展知识库

1. 在 `KnowledgeBaseTools.java` 的 `initializeDocuments()` 方法中添加新文档
2. 或者实现外部文档导入功能

### 自定义Agent

1. 修改 `CustomerServiceAgentConfig.java` 中的系统提示词
2. 调整Agent的行为和规则

## 项目配置

### application.yml

主要配置项说明：

- `server.port`: 应用端口，默认8080
- `agentscope.dashscope.api-key`: DashScope API密钥
- `agentscope.session.path`: 会话数据存储路径
- `agentscope.agent.model.name`: 使用的模型名称，默认qwen3-max

## 故障排除

### 常见问题

1. **API密钥错误**
   - 确保已正确设置 `DASHSCOPE_API_KEY` 环境变量
   - 检查API密钥是否有效

2. **模型调用失败**
   - 检查网络连接
   - 确认DashScope服务是否可用

3. **知识库初始化失败**
   - 检查嵌入模型配置
   - 确认API密钥是否有相关权限

## 许可证

本项目采用MIT许可证，详情请见LICENSE文件。

## 联系方式

如有问题或建议，请提交Issue或联系项目维护者。