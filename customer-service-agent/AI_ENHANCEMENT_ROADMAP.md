# 🚀 AI增强功能路线图

> 智能客服Agent系统 - AI能力提升计划
> 
> 创建日期：2024年
> 最后更新：2024年

---

## 📋 概述

本文档记录了AI Agent系统的相关增强功能，按照优先级排序，用于高级开发面试准备。每个功能都标注了面试价值、实现难度和实现思路。

**当前项目技术栈**：Spring Boot 3.1.5 + Java 17 + AgentScope Java + Milvus

---

## 🎯 功能优先级一览

| ID | 功能 | 优先级 | 状态 | 面试价值 | 难度 | 预估时间 |
|----|------|--------|------|----------|------|----------|
| 1 | Agent流式响应（SSE） | 🥇 P0 | ✅ 已完成 | ⭐⭐⭐⭐⭐ | ⭐⭐ | 2-3h |
| 2 | Agent长期记忆 | 🥇 P0 | ⏳ 待开始 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 3-4h |
| 3 | 成本控制（Token监控） | 🥇 P0 | ⏳ 待开始 | ⭐⭐⭐⭐ | ⭐⭐ | 2-3h |
| 4 | Prompt工程优化 | 🥈 P1 | ⏳ 待开始 | ⭐⭐⭐⭐ | ⭐ | 1-2h |
| 5 | 向量检索优化 | 🥈 P1 | ⏳ 待开始 | ⭐⭐⭐⭐ | ⭐⭐⭐ | 4-5h |
| 6 | 并发Agent实例优化 | 🥈 P1 | ⏳ 待开始 | ⭐⭐⭐ | ⭐⭐ | 2-3h |
| 7 | Agent编排&多Agent协作 | 🥉 P2 | ⏳ 待开始 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 1-2天 |
| 8 | Tool动态加载 | 🥉 P2 | ⏳ 待开始 | ⭐⭐⭐ | ⭐⭐⭐ | 3-4h |
| 9 | Few-shots上下文示例 | 🥉 P2 | ⏳ 待开始 | ⭐⭐⭐ | ⭐ | 1-2h |
| 10 | 多模态支持 | 🎪 P3 | ⏳ 待开始 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 2-3天 |

**图例**：
- 🥇 P0: 第一优先级 - 核心体验（强烈建议先做）
- 🥈 P1: 第二优先级 - 能力增强
- 🥉 P2: 第三优先级 - 高级特性
- 🎪 P3: 第四优先级 - 锦上添花
- ⏳ 待开始：还没开始实现
- 🔄 进行中：正在实现
- ✅ 已完成：已经完成

---

## 🥇 第一优先级 - 核心体验

### 1. Agent流式响应（SSE）

**状态**：✅ 已完成 | **优先级**：🥇 P0 | **完成时间**：约2小时

#### 面试价值 ⭐⭐⭐⭐⭐
- 用户体验大幅提升，打字机效果展示实时生成
- 技术点新颖：Server-Sent Events、Flux响应式编程
- 体现对用户体验的关注和前沿技术的应用

#### 实现难度 ⭐⭐
- 难度较低，WebFlux原生支持SSE
- 主要工作是改造Controller和返回类型

#### 推荐理由
✨ **用户体验核心** - 用户感知最明显，第一印象好
✨ **技术展示** - 非阻塞I/O、响应式编程
✨ **实现优雅** - Spring WebFlux + SSE结合紧密
✨ **周期短** - 2-3小时即可完成，快速见效

#### 实现思路

1. **改造Controller返回类型**
   ```java
   @GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
   public Flux<String> streamMessage(@RequestBody ChatRequest request) {
       return chatSessionService.streamUserMessage(userId, message);
   }
   ```

2. **Service层实现流式处理**
   ```java
   public Flux<String> streamUserMessage(String userId, String message) {
       ReActAgent agent = getUserSession(userId);
       Msg userMsg = createMsg(userId, message);
       return agent.streamCall(userMsg).map(this::formatEvent);
   }
   ```

3. **前端SSE接收**
   ```javascript
   const eventSource = new EventSource('/api/chat/stream');
   eventSource.onmessage = (event) => {
       displayStreamText(event.data); // 打字机效果
   };
   ```

4. **关键文件**
   - `ChatController.java` - 新增stream端点
   - `ChatSessionService.java` - 新增streamUserMessage方法
   - `stream.html` - 前端演示页面

#### 面试亮点
- ✅ 非阻塞I/O技术（Reactive Programming）
- ✅ Server-Sent Events（SSE）实战应用
- ✅ Spring WebFlux深入使用
- ✅ 用户体验优化思维
- ✅ 实时数据处理能力

#### 注意事项
- 需要AgentScope支持流式API
- 处理连接断开重连
- 限制流式超时时间
- 考虑前端缓冲和显示优化

---

### 2. Agent长期记忆（持久化存储）

**状态**：⏳ 待开始 | **优先级**：🥇 P0 | **预估时间**：3-4小时

#### 面试价值 ⭐⭐⭐⭐⭐
- 跨会话上下文保留，真正"记住用户"
- 体现架构设计能力（持久化、加载、检索）
- AI应用的核心难点和亮点功能

#### 实现难度 ⭐⭐⭐
- 需要设计持久化方案（数据库/文件）
- 需要考虑性能和容量

#### 推荐理由
🧠 **多会话上下文** - AI应用的核心能力差异点
🧠 **架构挑战** - 持久化、分区、加载策略
🧠 **实际需求** - 用户希望系统能记住对话历史
🧠 **技术深度** - 涉及数据库、序列化、内存管理

#### 实现思路

1. **记忆存储方案选择**
   - 方案A：数据库存储（推荐）- 适合生产环境
   - 方案B：文件存储（JSON）- 适合演示

2. **数据结构设计**
   ```java
   // 会话记忆实体
   {
     "userId": "user123",
     "sessionId": "session-001",
     "memories": [
       {
         "timestamp": "2024-01-15T10:00:00",
         "role": "USER",
         "content": "查询订单ORD001"
       },
       {
         "timestamp": "2024-01-15T10:00:05",
         "role": "ASSISTANT",
         "content": "订单已发货..."
       }
     ],
     "summary": "用户关心订单ORD001的物流状态",
     "userPreferences": {
       "focusOn": ["订单状态", "物流"]
     }
   }
   ```

3. **持久化服务**
   ```java
   @Service
   public class MemoryPersistenceService {
       
       // 保存用户记忆
       public void saveUserMemory(String userId, List<Msg> memories);
       
       // 加载用户记忆
       public List<Msg> loadUserMemory(String userId);
       
       // 生成摘要（可选）
       public String generateSummary(List<Msg> memories);
       
       // 智能记忆裁剪（保留重要记忆）
       public List<Msg> pruneMemories(List<Msg> memories);
   }
   ```

4. **记忆加载策略**
   - 冷启动：加载用户的最近N条记忆
   - 热加载：加载完整对话历史
   - 智能加载：根据当前问题检索相关历史记忆

5. **集成到ChatSessionService**
   ```java
   public ReActAgent getUserSession(String userId) {
       return userSessions.computeIfAbsent(userId, uid -> {
           // 1. 加载历史记忆
           List<Msg> history = memoryPersistence.loadUserMemory(uid);
           
           // 2. 恢复到Agent内存
           ReActAgent agent = createUserAgent(uid);
           agent.getMemory().addMessages(history);
           
           // 3. 设置摘要和偏好
           agent.setSummary(memoryPersistence.getSummary(uid));
           
           return agent;
       });
   }
   
   // 对话结束时保存记忆
   public void saveUserMemory(String userId) {
       ReActAgent agent = getUserSession(userId);
       memoryPersistence.saveUserMemory(userId, agent.getMemory().getAll());
   }
   ```

6. **关键文件**
   - `MemoryPersistenceService.java` - 记忆持久化服务
   - `MemoryEntity.java` - 记忆实体类
   - `MemoryRepository.java` - 数据访问层
   - `ChatSessionService.java` - 集成记忆加载/保存
   - `V1__create_memory_table.sql` - 数据库迁移脚本

#### 面试亮点
- ✅ 跨会话上下文保留能力
- ✅ 持久化架构设计
- ✅ 记忆管理策略（容量控制、摘要生成）
- ✅ 数据库设计优化
- ✅ AI应用的核心难点解决

#### 踩坑指南
- ⚠️ 记忆容量控制（避免无限增长）
- ⚠️ 隐私保护（加密存储敏感信息）
- ⚠️ 性能优化（批量加载、懒加载）
- ⚠️ 会话隔离（多端登录冲突）

---

### 3. 成本控制（Token监控与降级）

**状态**：⏳ 待开始 | **优先级**：🥇 P0 | **预估时间**：2-3小时

#### 面试价值 ⭐⭐⭐⭐
- 体现企业级思维和成本意识
- 监控告警体系设计
- 降级策略展示工程化能力

#### 实现难度 ⭐⭐
- Token统计比较简单
- 降级策略需要设计

#### 推荐理由
💰 **企业级思维** - 不是简单调用API，考虑成本
💰 **监控体系** - 体现系统性思维
💰 **降级策略** - 高可用设计
💰 **数据驱动** - 成本分析和优化

#### 实现思路

1. **Token统计数据结构**
   ```java
   {
     "userId": "user123",
     "totalTokens": 12500,
     "inputTokens": 7200,
     "outputTokens": 5300,
     "callCount": 25,
     "lastCallTime": "2024-01-15T10:30:00",
     "cost": 0.05
   }
   ```

2. **Token监控服务**
   ```java
   @Service
   public class TokenUsageMonitor {
       
       // 记录Token使用
       public void recordTokenUsage(String userId, int inputTokens, int outputTokens);
       
       // 获取用户Token统计
       public TokenStats getUserStats(String userId);
       
       // 检查是否超过阈值
       public boolean isOverThreshold(String userId);
       
       // 获取系统总成本
       public double getTotalCost();
       
       // 计算成本（根据价格表）
       public double calculateCost(String model, int inputTokens, int outputTokens);
   }
   ```

3. **降级策略设计**
   ```java
   @Service
   public class CostControlStrategy {
       
       // 策略1：模型降级（从高端模型降级到中端模型）
       public String getModelForUser(String userId) {
           if (monitor.isOverThreshold(userId)) {
               return "qwen-turbo"; // 廉价模型
           }
           return "qwen-max"; // 高端模型
       }
       
       // 策略2：功能降级（禁用某些tool调用）
       public List<Tool> getEnabledTools(String userId) {
           if (monitor.isOverThreshold(userId)) {
               return basicTools; // 仅保留基础工具
           }
           return allTools;
       }
       
       // 策略3：响应截断（限制输出长度）
       public int getMaxTokens(String userId) {
           if (monitor.isOverThreshold(userId)) {
               return 1000; // 短响应
           }
           return 4000; // 长响应
       }
   }
   ```

4. **集成到Agent调用**
   ```java
   public Msg processUserMessage(String userId, String userMessage) {
       // 1. 检查成本阈值
       if (monitor.isOverThreshold(userId)) {
           logger.warn("用户{}成本超限，应用降级策略", userId);
       }
       
       // 2. 动态选择模型
       String model = costControlStrategy.getModelForUser(userId);
       
       // 3. 动态配置工具
       Toolkit toolkit = costControlStrategy.getEnabledTools(userId);
       
       // 4. 调用并记录Token
       Msg response = agent.call(userMsg).block();
       monitor.recordTokenUsage(userId, 
           response.getUsage().inputTokens(),
           response.getUsage().outputTokens());
       
       return response;
   }
   ```

5. **监控接口**
   ```java
   @RestController
   @RequestMapping("/api/admin/cost")
   public class CostMonitoringController {
       
       // 获取用户成本统计
       @GetMapping("/user/{userId}")
       public TokenStats getUserCost(@PathVariable String userId);
       
       // 获取系统总成本
       @GetMapping("/total")
       public CostReport getTotalCost();
       
       // 设置用户成本阈值
       @PostMapping("/user/{userId}/threshold")
       public void setThreshold(@PathVariable String userId, @RequestBody int threshold);
       
       // 成本趋势（按天/周/月）
       @GetMapping("/trend")
       public List<CostTrendItem> getTrend(@RequestParam String period);
   }
   ```

6. **关键文件**
   - `TokenUsageMonitor.java` - Token监控服务
   - `TokenStats.java` - 统计数据实体
   - `CostControlStrategy.java` - 降级策略
   - `ChatSessionService.java` - 集成成本控制
   - `CostMonitoringController.java` - 管理接口
   - `cost-monitoring.html` - 前端Dashboard

#### 面试亮点
- ✅ 企业级成本意识
- ✅ 监控告警体系设计
- ✅ 降级策略实现
- ✅ 数据分析和报表
- ✅ 系统性工程思维

#### 成本参考（示例）
| 模型 | 输入价格/1K tokens | 输出价格/1K tokens | 说明 |
|------|-------------------|-------------------|------|
| qwen-max | ¥0.02 | ¥0.06 | 高端模型 |
| qwen-plus | ¥0.008 | ¥0.02 | 中端模型 |
| qwen-turbo | ¥0.003 | ¥0.006 | 廉价模型 |

#### 降级策略示例
```java
// 阈值配置
cost.thresholds:
  warning: 10000      // 警告阈值
  critical: 50000     // 严重阈值
  blocked: 100000     // 阻断阈值

// 降级动作
strategies:
  - level: warning
    actions:
      - model_downgrade: qwen-plus
      - max_tokens: 2000
  
  - level: critical
    actions:
      - model_downgrade: qwen-turbo
      - max_tokens: 1000
      - disable_tools: [add_knowledge, retrieve_knowledge]
  
  - level: blocked
    actions:
      - block_user: true
      - message: "今日额度已用完，请明天再来"
```

---

## 🥈 第二优先级 - 能力增强

### 4. Prompt工程优化

**状态**：⏳ 待开始 | **优先级**：🥈 P1 | **预估时间**：1-2小时

#### 面试价值 ⭐⭐⭐⭐
- 提升模型理解能力和回答质量
- 体现对LLM工作原理的深入理解

#### 实现难度 ⭐
- 主要是文本优化，不需要大量代码

#### 优化方向

1. **System Prompt重构**
   - 更清晰的角色定义
   - 明确的任务边界
   - 具体的输出格式要求
   - 举例和反例说明

2. **Few-shots示例嵌入**
   ```
   示例1：
   用户：查询订单ORD001状态
   助手：[调用query_order_status工具]
   
   示例2：
   用户：如何申请退款？
   助手：[调用search_knowledge_base工具]
   ```

3. **思维链提示（CoT）**
   ```
   解题步骤：
   1. 分析用户意图
   2. 判断需要的工具
   3. 调用工具获取信息
   4. 整理回答
   ```

4. **关键文件**
   - `Prompts.java` - 集中管理所有Prompt模板

---

### 5. 向量检索优化

**状态**：⏳ 待开始 | **优先级**：🥈 P1 | **预估时间**：4-5小时

#### 面试价值 ⭐⭐⭐⭐
- 提升知识库检索准确性
- 体现对RAG技术的深入理解

#### 实现难度 ⭐⭐⭐
- 需要理解向量检索原理
- 需要尝试不同的检索策略

#### 优化方案

1. **混合检索（关键词+向量）**
   - 结合BM25和向量相似度
   - 加权融合两种结果

2. **重排序（Rerank）**
   - 使用交叉编码器重新排序
   - 提升Top-K结果质量

3. **检索策略优化**
   - 动态调整检索数量
   - 根据问题类型选择检索策略

4. **关键文件**
   - `HybridRetriever.java` - 混合检索器
   - `Reranker.java` - 重排序器
   - `RetrievalStrategy.java` - 检索策略

---

### 6. 并发Agent实例优化

**状态**：⏳ 待开始 | **优先级**：🥈 P1 | **预估时间**：2-3小时

#### 面试价值 ⭐⭐⭐
- 优化多用户并发场景
- 提升系统吞吐量

#### 实现难度 ⭐⭐
- 需要考虑线程安全和资源管理

#### 优化方向

1. **Agent实例池化**
   - 复用Agent实例减少初始化开销
   - 动态扩缩容

2. **内存管理**
   - 控制最大并发数
   - 限制内存使用

3. **关键文件**
   - `AgentPoolManager.java` - Agent实例池
   - `AgentConfig.java` - 池化配置

---

## 🥉 第三优先级 - 高级特性

### 7. Agent编排&多Agent协作

**状态**：⏳ 待开始 | **优先级**：🥉 P2 | **预估时间**：1-2天

#### 面试价值 ⭐⭐⭐⭐⭐
- 架构级别的亮点
- 复杂场景的解决方案

#### 实现难度 ⭐⭐⭐⭐
- 需要设计Agent之间的通信和协作机制

#### 实现思路

1. **多Agent架构**
   ```
   User → DispatcherAgent → 
         ├─ OrderAgent（订单相关）
         ├─ KnowledgeAgent（知识相关）
         └─ GeneralAgent（通用问题）
         ↓
     SummaryAgent（汇总结果）
   ```

2. **Agent通信机制**
   - 共享Memory
   - 消息传递
   - 事件总线

3. **关键文件**
   - `AgentOrchestrator.java` - Agent编排器
   - `DispatcherAgent.java` - 分发Agent
   - `SummaryAgent.java` - 汇总Agent

---

### 8. Tool动态加载

**状态**：⏳ 待开始 | **优先级**：🥉 P2 | **预估时间**：3-4小时

#### 面试价值 ⭐⭐⭐
- 体现系统可扩展性
- 热更新能力

#### 实现难度 ⭐⭐⭐
- 需要设计工具注册和管理机制

#### 实现方案

1. **工具注册中心**
   ```java
   @Service
   public class ToolRegistry {
       private Map<String, ToolProvider> providers;
       
       public void registerTool(String toolId, ToolProvider provider);
       public void unregisterTool(String toolId);
       public Tool getTool(String toolId);
   }
   ```

2. **动态加载**
   - 从数据库加载工具配置
   - 支持运行时启用/禁用工具

3. **关键文件**
   - `ToolRegistry.java` - 工具注册中心
   - `ToolProvider.java` - 工具提供者接口

---

### 9. Few-shots上下文示例

**状态**：⏳ 待开始 | **优先级**：🥉 P2 | **预估时间**：1-2小时

#### 面试价值 ⭐⭐⭐
- 提升模型理解能力
- 技术点简单但效果明显

#### 实现方案

1. **示例管理**
   ```java
   @Configuration
   public class FewShotsConfig {
       private List<Example> orderExamples = List.of(
           new Example("查询ORD001状态", "调用query_order_status工具"),
           new Example("订单没收到", "调用query_shipping_status工具")
       );
   }
   ```

2. **动态注入**
   - 根据用户问题类型选择相关示例
   - 动态拼接到Prompt中

---

## 🎪 第四优先级 - 锦上添花

### 10. 多模态支持

**状态**：⏳ 待开始 | **优先级**：🎪 P3 | **预估时间**：2-3天

#### 面试价值 ⭐⭐⭐⭐
- 前沿技术展示
- 应用场景更丰富

#### 实现难度 ⭐⭐⭐⭐⭐
- 需要集成多种模型（视觉、语音）

#### 实现方向

1. **图片输入**
   - 上传图片
   - OCR识别
   - 视觉理解（GPT-4V等）

2. **语音输入**
   - 语音转文字
   - 文字转语音（TTS）

3. **关键文件**
   - `ImageInputController.java` - 图片输入接口
   - `SpeechInputController.java` - 语音输入接口

---

## 📊 功能对比矩阵

| 功能 | 技术深度 | 架构思维 | 面试印象 | 实现周期 | 推荐度 |
|------|----------|----------|----------|----------|--------|
| 1. 流式响应 | ⭕⭐⭐⭐⭕ | ⭕⭐⭐⭕⭕ | ⭐⭐⭐⭐⭐ | 2-3小时 | ⭐⭐⭐⭐⭐ |
| 2. 长期记忆 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 3-4小时 | ⭐⭐⭐⭐⭐ |
| 3. 成本控制 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 2-3小时 | ⭐⭐⭐⭐⭐ |
| 4. Prompt优化 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | 1-2小时 | ⭐⭐⭐ |
| 5. 向量检索优化 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 4-5小时 | ⭐⭐⭐⭐ |
| 6. 并发Agent | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 2-3小时 | ⭐⭐⭐ |
| 7. Agent编排 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 1-2天 | ⭐⭐⭐⭐ |
| 8. 动态加载 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 3-4小时 | ⭐⭐⭐ |
| 9. Few-shots | ⭐⭐ | ⭐⭐ | ⭐⭐⭐ | 1-2小时 | ⭐⭐⭐ |
| 10. 多模态 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 2-3天 | ⭐⭐⭐⭐ |

---

## 🎯 推荐实施阶段

### 📅 第一阶段（本周完成）- 核心体验 ✨

**目标**：快速提升用户体验和面试竞争力

| # | 功能 | 工作量 | 依赖 |
|---|------|--------|------|
| 1 | Agent流式响应（SSE） | 2-3h | 无 |
| 2 | Agent长期记忆 | 3-4h | 无 |
| 3 | 成本控制（Token监控） | 2-3h | 无 |
| **总计** | **7-10小时** | |

**产出**：
- 用户可以实时看到Agent回答（打字机效果）
- Agent能记住历史对话（多会话上下文）
- 有成本监控和降级策略（企业级功能）

**面试亮点**：
- ✅ 响应式编程 + SSE
- ✅ 持久化架构设计
- ✅ 成本意识和降级策略

---

### 📅 第二阶段（下周完成）- 能力增强 ⚡

**目标**：提升Agent的智能和性能

| # | 功能 | 工作量 | 依赖 |
|---|------|--------|------|
| 4 | Prompt工程优化 | 1-2h | 无 |
| 6 | 并发Agent实例优化 | 2-3h | 无 |
| 5 | 向量检索优化 | 4-5h | 2 |
| **总计** | **7-10小时** | |

**产出**：
- 模型理解能力提升
- 知识库检索更准确
- 多用户并发性能优化

**面试亮点**：
- ✅ Prompt工程实践
- ✅ RAG优化策略
- ✅ 并发性能优化

---

### 📅 第三阶段（有时间再做）- 高级特性 🔥

**目标**：展示复杂场景的处理能力

| # | 功能 | 工作量 | 依赖 |
|---|------|--------|------|
| 7 | Agent编排&多Agent协作 | 1-2天 | 2, 6 |
| 8 | Tool动态加载 | 3-4h | 无 |
| 9 | Few-shots上下文示例 | 1-2h | 4 |
| **总计** | **1.5-2天** | |

**产出**：
- 多Agent协作能力
- 工具可动态管理
- 智能性进一步提升

**面试亮点**：
- ✅ 复杂架构设计
- ✅ 系统可扩展性
- ✅ Agent编排能力

---

### 📅 第四阶段（可选）- 锦上添花 💎

| # | 功能 | 工作量 | 依赖 |
|---|------|--------|------|
| 10 | 多模态支持 | 2-3天 | 1, 3 |
| **总计** | **2-3天** | |

**产出**：
- 支持图片输入
- 支持语音交互

**面试亮点**：
- ✅ 前沿技术应用
- ✅ 多模态AI能力

---

## 📝 实施检查清单

### 开始前准备

- [ ] 确认AgentScope支持流式API
- [ ] 准备开发环境（数据库、Redis等）
- [ ] 创建feature分支
- [ ] 阅读相关文档

### 每个功能完成后

- [ ] 代码审查
- [ ] 单元测试
- [ ] 集成测试
- [ ] 文档更新
- [ ] 提交流程提交

### 验收标准

- [ ] 功能正常工作
- [ ] 有单元测试覆盖
- [ ] 代码符合规范
- [ ] 有清晰的注释
- [ ] 更新了相关文档

---

## 🎓 学习资源

### 流式响应（SSE）
- [Spring WebFlux官方文档](https://docs.spring.io/spring-framework/reference/web/webflux/reactive-spring.html)
- [MDN - Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)

### 长期记忆
- [LangChain Memory概念](https://python.langchain.com/docs/modules/memory/)
- [向量数据库最佳实践](https://zilliz.com/learn/what-is-vector-database)

### Prompt工程
- [OpenAI Prompt工程指南](https://platform.openai.com/docs/guides/prompt-engineering)
- [Few-shot Learning示例](https://arxiv.org/abs/2005.14165)

### 向量检索优化
- [混合检索技术](https://zilliz.com/learn/hybrid-search-vectors-keywords)
- [重排序（Reranking）](https://www.pinecone.io/learn/series/learning-vector-databases/)

---

## 📈 进度跟踪

### 活跃功能

| 功能 | 负责人 | 开始日期 | 完成日期 | 状态 | 备注 |
|------|--------|----------|----------|------|------|
| - | - | - | - | - | - |

### 已完成

| 功能 | 完成日期 | 代码提交号 | 验收通过 | 备注 |
|------|----------|-----------|----------|------|
| - | - | - | - | - |

### 待开始

| 功能 | 计划开始 | 计划完成 | 优先级 | 阻塞因素 |
|------|----------|----------|--------|----------|
| 1. Agent流式响应 | - | - | 🥇 P0 | 无 |
| 2. Agent长期记忆 | - | - | 🥇 P0 | 无 |
| 3. 成本控制 | - | - | 🥇 P0 | 无 |

---

## 💡 使用建议

### 如何使用本路线图

1. **按优先级实施**：优先完成第一阶段的3个核心功能
2. **逐步推进**：每个功能完成后进行代码审查和测试
3. **及时更新**：完成后更新本文件的状态和进度
4. **保持沟通**：遇到问题及时记录和学习

### 面试准备建议

1. **技术理解**：深入理解每个功能的原理和实现
2. **优化思路**：思考还可以如何优化
3. **扩展能力**：思考哪些功能可以扩展或结合
4. **业务价值**：清楚每个功能的业务价值

---

## 🚀 快速开始

**建议第一个实现的功能**：Agent流式响应（SSE）

**原因**：
- 实现周期短（2-3小时）
- 用户体验提升明显
- 技术点新颖且实用
- 面试效果极佳

**开始命令**：
```bash
# 创建功能分支
git checkout -b feature/sse-chat

# 开始实现...
```

---

## 📞 联系人

**项目维护者**：[待填写]
**技术支持**：[待填写]
**问题反馈**：[待填写]

---

**祝你编码愉快！🎉**