# GraphRAG 智能客服系统 - 面试讲解指南

## 一、项目背景

### 1.1 我要解决什么问题

这是一个**智能客服系统 Demo**，用于面试展示。原始系统基于**纯向量检索 RAG**（Vector RAG），存在以下问题：

| 问题 | 说明 |
|------|------|
| 语义鸿沟 | 用户问题"手表坏了怎么办"可能匹配到无关的"手机坏了怎么办" |
| 孤立知识 | 无法表达"智能手表 → 有保修服务 → 保修范围包含两年"这样的关联关系 |
| 黑盒检索 | 不知道为什么会召回这条知识，无法解释 |
| 多跳推理弱 | 无法回答"买了某产品后相关服务是什么"这种需要推理的问题 |

### 1.2 我的解决方案

引入**知识图谱 + GraphRAG**，实现：

```
用户问题 → 知识图谱检索 → 结构化关系推理 → 可解释的答案
```

**核心目标**：
1. 结构化知识管理 - 从扁平文本进化为实体-关系网络
2. 问答增强 - 支持多跳推理和因果关系问答
3. 前端可视化 - 交互式知识图谱展示
4. 效果对比 - 证明 GraphRAG 优于纯向量 RAG

---

## 二、整体架构

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         前端 (Vue 3)                              │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐    │
│  │  知识库管理    │  │  图谱可视化   │  │  对比实验界面       │    │
│  │  (增强)      │  │  (新增)      │  │  (Graph vs Vector)│    │
│  └──────────────┘  └──────────────┘  └────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      后端 (Spring Boot)                            │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐    │
│  │  知识抽取管道   │  │  GraphRAG    │  │  对比评测服务       │    │
│  │  (规则+LLM)   │  │  检索服务     │  │  (同一问题双路径)   │    │
│  └──────────────┘  └──────────────┘  └────────────────────┘    │
│          │                │                   │                  │
│          ▼                ▼                   ▼                  │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐    │
│  │ SimpleKnowledge│  │   Neo4j      │  │     Milvus         │    │
│  │   (现有)      │  │   (新增)      │  │     (现有)         │    │
│  └──────────────┘  └──────────────┘  └────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流转

```
知识条目文本
    │
    ▼
┌─────────────────────┐
│    TripleExtractor    │  ← 混合抽取管道
│  ┌─────────────────┐ │
│  │ RulePreprocessor │ │  ← 规则预处理（正则）
│  └────────┬────────┘ │
│           │          │
│           ▼          │
│  ┌─────────────────┐ │
│  │ LLMTripleExtractor│ │  ← LLM抽取（M2.7）
│  └────────┬────────┘ │
└───────────┼──────────┘
            │
            ▼
     (实体, 关系, 实体) 三元组
            │
            ▼
┌─────────────────────────────────────┐
│              Neo4j                    │
│  (:Product)-[:HAS_SERVICE]->(:Service)│
│  (:QA)-[:RELATED_TO]->(:Product)     │
└─────────────────────────────────────┘
```

---

## 三、核心模块讲解

### 3.1 知识抽取管道

**设计思路**：规则抽取快但覆盖有限，LLM抽取准但慢，所以采用**混合策略**。

```
输入: "智能手表如何保修？\n答案：联系客服处理。\n产品：智能手表\n保修：两年质保"

Step 1: 规则预处理（正则快速提取）
├── 产品: ["智能手表"]
├── 服务: ["保修", "两年质保"]
├── 订单: []
└── 问答: [{"question": "智能手表如何保修", "answer": "联系客服处理"}]

Step 2: 转换为三元组
├── (智能手表, MENTIONS, "智能手表如何保修")
├── ("智能手表如何保修", HAS_SERVICE, 保修)
├── ("智能手表如何保修", HAS_SERVICE, 两年质保)
└── ...

Step 3: LLM深层抽取
└── 调用MiniMax M2.7，抽取隐含关系...

Step 4: 推断实体类型并存入Neo4j
├── "智能手表" → Product
├── "保修" → Service
└── "智能手表如何保修" → QA
```

**关键代码**：

```java
// TripleExtractor.extractAndStore()
public void extractAndStore(String knowledgeEntryId, String title, String content) {
    // 1. 规则预提取
    Map<String, Object> preprocessed = rulePreprocessor.preprocess(fullText);

    // 2. 转换为三元组
    for (String product : products) {
        triples.add(Map.of("subject", product, "relation", "MENTIONS", "object", title));
    }

    // 3. LLM深层抽取
    List<Map<String, String>> llmTriples = llmTripleExtractor.extractTriples(fullText);

    // 4. 存储到Neo4j
    for (Map<String, String> triple : triples) {
        knowledgeGraphService.addTriple(subject, subjectType, relation, object, objectType);
    }
}
```

### 3.2 GraphRAG 检索流程

**核心思想**：通过知识图谱的结构化关系进行检索，而不是单纯的语义相似度。

```
用户问题: "智能手表有什么保修服务？"
    │
    ├─► 规则匹配 ─→ 关键词: "智能手表"(Product), "保修"(Service)
    │
    └─► 图谱遍历 ─→ 查找 Product:"智能手表" 1-3跳内的所有关联
                        │
                        ▼
                  ┌───────────────┐
                  │ 智能手表       │ ← Product
                  │  │            │
                  │  │ HAS_SERVICE │
                  │  ▼            │
                  │ 两年质保       │ ← Service
                  │  │            │
                  │  │ RELATED_TO  │
                  │  ▼            │
                  │ 如何保修？     │ ← QA
                  └───────────────┘
                        │
                        ▼
                 返回子图 + 答案上下文
```

**关键代码**：

```java
// GraphRAGRetriever.search()
public GraphSearchResult search(String query, int limit) {
    // Step 1: 规则匹配 - 快速定位实体
    Set<String> matchedEntityIds = ruleBasedMatch(query);

    // Step 2: 如果没匹配到，用LLM做实体链接
    if (matchedEntityIds.isEmpty()) {
        matchedEntityIds = llmEntityLinking(query);
    }

    // Step 3: 构建子图（N跳范围内的关联节点和边）
    Map<String, Object> subgraph = buildSubgraph(matchedEntityIds, MAX_HOPS);

    // Step 4: 生成答案上下文
    String answer = generateAnswerContext(query, matchedEntityIds);

    return new GraphSearchResult(answer, retrievedEntities, nodes, edges);
}
```

### 3.3 混合检索策略

**为什么要混合？** 两者各有优劣，混合能取长补短。

| 检索方式 | 优势 | 劣势 |
|----------|------|------|
| Vector RAG | 语义理解强、部署简单 | 无法推理、结构化差 |
| Graph RAG | 可解释、能推理、关系清晰 | 依赖图谱质量、召回可能不全 |

**并行执行 + 加权融合**：

```java
public HybridSearchResult hybridSearch(String query, int limit) {
    // 并行执行，不阻塞
    CompletableFuture<VectorSearchResult> vectorFuture =
        CompletableFuture.supplyAsync(() -> vectorRAG.search(query));

    CompletableFuture<GraphSearchResult> graphFuture =
        CompletableFuture.supplyAsync(() -> graphRAG.search(query));

    // 等待结果
    VectorSearchResult vectorResult = vectorFuture.join();
    GraphSearchResult graphResult = graphFuture.join();

    // 融合时按权重分配重要性
    // Score = α * VectorScore + (1-α) * GraphScore
    return new HybridSearchResult(vectorResult, graphResult);
}
```

---

## 四、知识图谱设计

### 4.1 实体类型

| 实体类型 | Label | 示例 |
|----------|-------|------|
| 产品 | `:Product` | 智能手表、蓝牙耳机 |
| 服务 | `:Service` | 保修、质保、退换 |
| 订单 | `:Order` | ORD20240315001 |
| 问答 | `:QA` | 如何保修？、怎么退款？ |
| 概念 | `:Concept` | 质量问题、维修政策 |

### 4.2 关系类型

| 关系 | 起点→终点 | 示例 |
|------|-----------|------|
| `BELONGS_TO` | Product→Category | 智能手表→电子产品 |
| `HAS_SERVICE` | Product→Service | 智能手表→两年质保 |
| `CONTAINS` | Order→Product | 订单ORD001→智能手表 |
| `RELATED_TO` | QA→Service | 如何保修？→两年质保 |
| `REFERENCES` | QA→Product | 如何保修？→智能手表 |
| `MENTIONS` | QA→Concept | 如何保修？→质量问题 |

### 4.3 Neo4j 查询示例

```cypher
-- 查找某个产品的所有关联服务和QA
MATCH (p:Product {name: "智能手表"})-[r]-(connected)
RETURN p, r, connected

-- 查找两跳内的所有关联
MATCH path = (n:Product {name: "智能手表"})-[*1..2]-(m)
RETURN path
```

---

## 五、前端展示

### 5.1 知识图谱可视化页 (`/graph`)

- 使用 **Cytoscape.js** 渲染图谱
- 节点按类型着色（Product蓝色、Service绿色、QA橙色）
- 支持缩放、拖拽、点击查看详情
- 刷新按钮重新加载图谱数据

### 5.2 对比实验页 (`/compare`)

```
┌────────────────────────────────────────────────────────────┐
│  问题: [智能手表有什么保修服务？]  [开始对比]               │
├────────────────────────┬─────────────────────────────────┤
│     Vector RAG         │         Graph RAG                │
├────────────────────────┼─────────────────────────────────┤
│ 答案:                  │ 答案:                           │
│ 智能手表保修两年...     │ 根据图谱：智能手表 → HAS_SERVICE │
│                        │ → 两年质保                       │
├────────────────────────┼─────────────────────────────────┤
│ 检索到的片段:           │ 检索到的实体:                    │
│ - 智能手表保修两年...   │ - Product: 智能手表              │
│ - 退换货政策说明...     │   → HAS_SERVICE → 两年质保      │
│                        │   → RELATED_TO → 如何保修？      │
├────────────────────────┴─────────────────────────────────┤
│                      迷你图谱可视化                         │
│            ◉ ──HAS_SERVICE── ◉ ──RELATED_TO── ◉          │
└────────────────────────────────────────────────────────────┘
```

---

## 六、技术亮点（面试加分项）

### 6.1 混合抽取策略

**规则 + LLM 双轨抽取**，既能快速响应，又能捕获深层语义：

- **规则抽取**：正则表达式，速度快，适合结构化文本
- **LLM抽取**：调用 MiniMax M2.7 进行深层语义理解
- **优先级**：规则结果直接用，LLM结果作为补充

### 6.2 可视化的对比实验

**同一问题，双路径检索**，直观展示 GraphRAG 的优势：

- Vector RAG：基于语义相似度，返回知识片段
- Graph RAG：基于图谱结构，返回关系路径
- 用户可以**肉眼对比**两者答案质量的差异

### 6.3 知识图谱的可解释性

```
Vector RAG: "根据相似度匹配，你的问题可能与以下知识相关..."
Graph RAG: "智能手表 → HAS_SERVICE → 两年质保 → RELATED_TO → 如何保修？
          这就是为什么我们得出这个答案的原因..."
```

### 6.4 技术选型的考量

| 选型 | 原因 |
|------|------|
| Neo4j | 成熟的图数据库，Cypher查询方便 |
| Cytoscape.js | 轻量级、API简洁、适合Vue集成 |
| MiniMax M2.7 | 国产LLM、面试展示友好 |

---

## 七、可能的追问

### Q1: 为什么不只用 GraphRAG？

**答**：GraphRAG 依赖知识图谱的质量。如果图谱稀疏或实体识别不准，召回会受影响。Vector RAG 作为互补，在图谱不完善时仍能提供基础的语义检索能力。

### Q2: LLM 抽取失败怎么办？

**答**：有降级策略。当 LLM 不可用或抽取失败时，规则抽取的结果仍能保证基本召回，只是覆盖率可能降低。

### Q3: 如何保证抽取质量？

**答**：两层保证：
1. 规则层：正则只能匹配明确格式的内容，不会乱抽
2. LLM层：设计了严格的 Prompt，要求只输出标准关系类型

### Q4: 性能如何？

**答**：混合检索是**并行执行**的，总时间 = max(Vector耗时, Graph耗时) 而不是相加。LLM 抽取是**异步**的，不阻塞主流程。

### Q5: 图谱可视化性能问题？

**答**：大图谱分页加载 + 按需渲染。初始只加载部分节点，缩放时才加载更多。

---

## 八、项目结构

```
customer-service-agent/
├── config/
│   └── Neo4jConfig.java          # Neo4j连接配置
├── controller/
│   ├── GraphApiController.java   # 图谱API
│   └── ComparisonController.java  # 对比实验API
├── dto/
│   ├── GraphNodeResponse.java    # 节点DTO
│   ├── GraphEdgeResponse.java     # 边DTO
│   ├── GraphSearchResult.java     # GraphRAG结果
│   ├── VectorSearchResult.java    # VectorRAG结果
│   └── ...
├── service/
│   ├── KnowledgeGraphService.java # Neo4j CRUD
│   ├── extractor/
│   │   ├── RulePreprocessor.java    # 规则预处理
│   │   ├── LLMTripleExtractor.java   # LLM抽取
│   │   └── TripleExtractor.java       # 混合抽取器
│   └── retriever/
│       ├── GraphRAGRetriever.java    # GraphRAG检索
│       └── HybridRAGService.java      # 混合检索

frontend/
├── src/
│   ├── api/index.js              # API函数
│   ├── stores/graph.js           # 图谱状态管理
│   ├── views/
│   │   ├── GraphView.vue         # 图谱可视化页
│   │   └── CompareView.vue       # 对比实验页
│   └── router/index.js           # 路由配置
```

---

## 九、总结

这是一个**GraphRAG 实战项目**，完整实现了：

1. **知识图谱构建** - 从知识条目自动抽取三元组存入Neo4j
2. **GraphRAG 检索** - 基于图谱结构的关系推理检索
3. **混合检索** - Vector RAG + Graph RAG 并行融合
4. **可视化对比** - 前端直观展示两种RAG的效果差异

核心亮点：**用面试官能理解的方式，展示了从0到1构建知识图谱RAG的完整思路**。
