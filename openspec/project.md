# Project Context

## Purpose
智能客服Agent系统，基于AgentScope Java框架构建的企业级客服系统，具备意图识别、多轮对话管理和知识库问答功能。

## Tech Stack
- **语言**: Java 17
- **后端框架**: Spring Boot 3.1.5
- **AI框架**: AgentScope Java 1.0.9
- **构建工具**: Maven
- **LLM提供商**: 阿里云百炼 (DashScope) / OpenAI兼容API
- **向量数据库**: Milvus 2.6.13
- **嵌入模型**: BAAI/bge-m3
- **PDF处理**: Apache PDFBox 2.0.27

## Project Conventions

### Code Style
- **Java编码规范**: 遵循Google Java Style Guide
- **包命名**: `com.example.customerservice`
- **日志使用**: SLF4J + Logback，使用`LoggerFactory.getLogger(ClassName.class)`
- **工具类注解**: 使用`@Component`让Spring管理Bean
- **Agent工具**: 使用`@Tool`和`@ToolParam`注解定义工具
- **方法命名**: 驼峰命名法(camelCase)
- **记录类**: 使用Java record定义简单数据传输对象(如Order)

### Architecture Patterns
- **分层架构**: Controller → Service → Tools
- **Spring Boot**: 主应用入口 + @Component扫描
- **Agent配置**: 集中于`CustomerServiceAgentConfig`
- **会话管理**: 基于AgentScope的Session机制，持久化到文件系统
- **知识库**: 基于AgentScope RAG功能，使用Milvus向量数据库

### Testing Strategy
- 使用Spring Boot Test进行单元测试
- Mock外部依赖进行隔离测试

### Git Workflow
- **分支策略**: 主分支(main) + 功能分支
- **提交规范**: 使用conventional commits格式
  - `feat:` 新功能
  - `fix:` 错误修复
  - `refactor:` 代码重构
  - `docs:` 文档更新
- **示例**: `feat:添加流式处理 更新待办清单`

## Domain Context
- **业务场景**: 电商客服
- **核心功能**:
  - 订单查询(订单状态、详情、物流)
  - 退款处理
  - 产品信息查询
  - 知识库问答(RAG)
- **用户交互**: REST API (JSON)
- **会话管理**: 多用户独立会话

## Important Constraints
- API密钥通过环境变量管理(DASHSCOPE_API_KEY, CHAT_API_KEY等)
- 会话数据存储在本地文件系统(`./sessions`)
- Milvus默认连接`localhost:19530`
- 需要兼容OpenAI API格式的LLM服务

## External Dependencies
- **DashScope API**: 阿里云百炼LLM服务
- **Milvus**: 向量数据库用于知识检索
- **AgentScope**: AI Agent框架(ReAct Agent模式)
