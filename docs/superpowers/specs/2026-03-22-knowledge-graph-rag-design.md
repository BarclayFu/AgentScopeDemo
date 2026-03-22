# Knowledge Graph RAG 设计方案

## 1. 目标与背景

### 1.1 项目定位
- **项目性质**: 智能客服系统 Demo，用于面试展示
- **核心目标**: 将现有纯向量 RAG 增强为 GraphRAG，实现知识图谱可视化和效果对比

### 1.2 预期收益
1. **结构化知识管理** - 从扁平文本进化为实体-关系网络
2. **问答增强** - 支持多跳推理和因果关系问答
3. **前端可视化** - 交互式知识图谱展示
4. **效果对比** - 证明 GraphRAG 优于纯向量 RAG

### 1.3 现有系统
- **向量检索**: Milvus + SimpleKnowledge (Vector-based RAG)
- **知识条目**: `data/knowledge-entries.json`，扁平文本格式
- **嵌入模型**: BAAI/bge-m3 (1024 维)

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────────┐   │
│  │ 知识库管理 │  │ 图谱可视化 │  │  对比实验界面 (Graph vs  │   │
│  │ (增强)   │  │  (新增)   │  │   Vector RAG)           │   │
│  └──────────┘  └──────────┘  └──────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     后端 (Spring Boot)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  知识抽取管道   │  │  GraphRAG    │  │  对比评测服务     │  │
│  │  (规则+LLM)   │  │  检索服务     │  │  (同一问题双路径) │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
│          │                │                   │            │
│          ▼                ▼                   ▼            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ SimpleKnowledge│  │ Neo4j        │  │  Milvus (现有)   │  │
│  │ (现有)        │  │ (新增)       │  │                  │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 新增组件

| 组件 | 位置 | 职责 |
|------|------|------|
| `KnowledgeGraphService` | `service/` | 知识图谱核心服务 |
| `TripleExtractor` | `service/extractor/` | 知识抽取管道 |
| `GraphRAGRetriever` | `service/retriever/` | GraphRAG 检索 |
| `ComparisonController` | `controller/` | 对比实验 API |
| `GraphApiController` | `controller/` | 图谱数据 API |
| `KnowledgeGraphView.vue` | `views/` | 图谱可视化页 |
| `ComparisonView.vue` | `views/` | 对比实验页 |

---

## 3. 数据模型

### 3.1 实体类型

| 实体类型 | 标签 | 属性 |
|----------|------|------|
| Product | `:Product` | name, category, price |
| Order | `:Order` | orderId, status |
| Service | `:Service` | type (warranty/return/repair) |
| QA | `:QA` | question, answer |
| Concept | `:Concept` | name (通用概念) |

### 3.2 关系类型

| 关系类型 | 起点 | 终点 | 说明 |
|----------|------|------|------|
| `:BELONGS_TO` | Product | Category | 产品属于分类 |
| `:HAS_SERVICE` | Product | Service | 产品有服务 |
| `:CONTAINS` | Order | Product | 订单包含产品 |
| `:RELATED_TO` | QA | Service | QA 关联服务 |
| `:REFERENCES` | QA | Product | QA 引用产品 |
| `:MENTIONS` | QA | Concept | QA 提及概念 |

### 3.3 Neo4j 图谱结构

```cypher
// 示例节点
(:Product {name: "智能手表", category: "电子产品", price: 2999})
(:Service {type: "warranty", description: "两年质保"})
(:QA {question: "如何保修？", answer: "联系客服..."})

// 示例关系
(:Product)-[:HAS_SERVICE]->(:Service)
(:QA)-[:RELATED_TO]->(:Service)
```

---

## 4. 知识抽取管道

### 4.1 抽取流程

```
知识条目文本
    │
    ▼
┌─────────────────┐
│  规则预处理       │  ← 提取结构化信息（Q&A格式、关键词）
│  (RulePreprocessor)│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  LLM 三元组抽取   │  ← 实体识别 + 关系抽取
│  (LLMTripleExtractor)│
└────────┬────────┘
         │
         ▼
    (实体, 关系, 实体) 三元组
         │
         ├─→ Neo4j 存储
         │
         └─→ 映射到 SimpleKnowledge (向后兼容)
```

### 4.2 规则预处理策略

```java
// 识别的模式
patterns:
  - "\\?(.*)\\n答：(.*)"  → Q&A 格式提取
  - "产品[：:](.*)"        → 产品名提取
  - "订单[：:](.*)"        → 订单号提取
  - "(保修|退换|维修)"     → 服务类型识别
```

### 4.3 LLM 三元组抽取 Prompt

```json
{
  "task": "从文本中抽取知识图谱三元组",
  "output_format": "JSON数组，每项为 {\"subject\": \"实体A\", \"relation\": \"关系\", \"object\": \"实体B\"}",
  "entities": ["Product", "Service", "QA", "Concept"],
  "relations": ["BELONGS_TO", "HAS_SERVICE", "RELATED_TO", "REFERENCES", "MENTIONS"]
}
```

### 4.4 抽取触发时机

| 时机 | 动作 |
|------|------|
| 新增知识条目 | 立即抽取并存入 Neo4j |
| 重建索引 | 全量重新抽取 |
| 初始化 | 从现有条目批量抽取 |

---

## 5. GraphRAG 检索服务

### 5.1 检索流程

```
用户问题
    │
    ├─→ 关键词规则匹配 → 定位起始实体
    │
    └─→ LLM 实体链接 → 识别问题中的实体
              │
              ▼
        图谱遍历 (2-3 hop)
              │
              ▼
        相关子图 → 返回上下文
```

### 5.2 检索配置

```java
// 检索参数
maxHops = 3           // 最大跳数
maxResults = 20      // 最大返回结果
scoreThreshold = 0.3 // 相似度阈值
```

### 5.3 Cypher 查询示例

```cypher
// 查找与"保修"相关的实体及2跳内关联
MATCH (start:Service {type: 'warranty'})
MATCH path = (start)-[*1..2]-(connected)
RETURN path, start, connected
LIMIT 50
```

### 5.4 与 Vector RAG 并行

```java
public class HybridRAGService {
    // 两条检索路径并行执行
    public RAGResult hybridSearch(String query) {
        CompletableFuture<VectorResult> vectorFuture =
            CompletableFuture.supplyAsync(() -> vectorRAG.search(query));

        CompletableFuture<GraphResult> graphFuture =
            CompletableFuture.supplyAsync(() -> graphRAG.search(query));

        // 合并结果
        return combineResults(vectorFuture.get(), graphFuture.get());
    }
}
```

---

## 6. 前端功能

### 6.1 知识图谱可视化页 (`/graph`)

**功能**:
- 全局图谱展示（所有实体和关系）
- 支持缩放、拖拽、筛选
- 点击实体查看详情
- 查询时高亮相关路径

**技术选型**:
- D3.js 或 Cytoscape.js（推荐 Cytoscape，API 更简洁）

### 6.2 对比实验页 (`/compare`)

**功能**:
- 输入问题，分别触发 Vector RAG 和 GraphRAG
- 并排展示两个答案
- 展示两个 RAG 各自检索到的知识片段
- 图谱可视化高亮相关路径（GraphRAG 侧）

**界面布局**:
```
┌────────────────────────────────────────────────────────┐
│  问题输入: [________________________] [开始对比]        │
├─────────────────────┬────────────────────────────────┤
│   Vector RAG         │   GraphRAG                      │
│   ─────────────      │   ──────────                    │
│   答案:              │   答案:                          │
│   ...               │   ...                           │
│                     │   + 图谱可视化高亮               │
│   检索到:            │   检索到:                        │
│   - 知识片段1       │   - 实体A → 关系 → 实体B        │
│   - 知识片段2       │   - 实体C                        │
└─────────────────────┴────────────────────────────────┘
```

---

## 7. API 设计

### 7.1 图谱数据 API

| 端点 | 方法 | 功能 |
|------|------|------|
| `/api/graph/stats` | GET | 图谱统计（节点数、边数） |
| `/api/graph/nodes` | GET | 获取所有节点（分页） |
| `/api/graph/edges` | GET | 获取所有边（分页） |
| `/api/graph/subgraph` | POST | 获取子图（给定中心节点和跳数） |
| `/api/graph/path` | POST | 查询两个实体间的路径 |

### 7.2 对比实验 API

| 端点 | 方法 | 功能 |
|------|------|------|
| `/api/compare/search` | POST | 同一问题双路径搜索 |
| `/api/compare/extract` | POST | 抽取知识条目为三元组（预览） |

---

## 8. 实现阶段

### Phase 1: Neo4j 集成
- [ ] Neo4j 数据库部署和连接配置
- [ ] 定义实体和关系模型
- [ ] 基础 CRUD 操作服务

### Phase 2: 知识抽取管道
- [ ] 规则预处理组件
- [ ] LLM 三元组抽取组件
- [ ] 抽取触发和同步机制

### Phase 3: GraphRAG 检索
- [ ] 实体链接和检索
- [ ] 图谱遍历和子图返回
- [ ] 与现有 Vector RAG 集成

### Phase 4: 前端展示
- [ ] 图谱可视化组件
- [ ] 对比实验界面
- [ ] 知识库管理增强

---

## 9. 技术依赖

### 后端新增
```xml
<dependency>
    <groupId>org.neo4j.driver</groupId>
    <artifactId>neo4j-java-driver</artifactId>
    <version>5.x</version>
</dependency>
```

### 前端新增
```json
{
  "dependencies": {
    "cytoscape": "^3.28.0"
  }
}
```

---

## 10. 风险与注意事项

1. **LLM 抽取延迟**: 三元组抽取会增加知识入库延迟，可考虑异步处理
2. **图谱稀疏性**: 初期知识量少，图谱可能不够稠密，需注意数据积累
3. **Neo4j 维护**: 需单独部署 Neo4j 服务，增加运维复杂度
4. **对比实验公平性**: 确保 Vector RAG 和 GraphRAG 在相同条件下对比
