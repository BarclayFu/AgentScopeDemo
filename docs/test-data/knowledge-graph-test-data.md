# 知识图谱测试数据

可以直接在 Neo4j Browser 中执行以下 Cypher 语句导入数据。

## 一、产品数据

```cypher
// 创建产品节点
CREATE
  (p1:Product {name: '智能手表', category: '电子产品', price: 2999}),
  (p2:Product {name: '蓝牙耳机', category: '电子产品', price: 899}),
  (p3:Product {name: '智能手机', category: '电子产品', price: 5999}),
  (p4:Product {name: '平板电脑', category: '电子产品', price: 3999}),
  (p5:Product {name: '智能手环', category: '电子产品', price: 399})
```

## 二、服务类型数据

```cypher
// 创建服务节点
CREATE
  (s1:Service {name: '两年质保', type: 'warranty', description: '两年内非人为损坏免费维修'}),
  (s2:Service {name: '七天无理由退换', type: 'return', description: '收货7天内可申请退换货'}),
  (s3:Service {name: '一年保修', type: 'warranty', description: '一年内免费保修'}),
  (s4:Service {name: '终身维修', type: 'repair', description: '终身提供维修服务，收取材料费'}),
  (s5:Service {name: '运费险', type: 'shipping', description: '退货时补贴运费'}),
  (s6:Service {name: '极速退款', type: 'refund', description: '审核通过后24小时内退款'})
```

## 三、QA数据

```cypher
// 创建问答节点
CREATE
  (q1:QA {name: '如何申请保修？', question: '如何申请保修？', answer: '请联系客服，提供订单号和产品问题描述，我们会在24小时内安排维修。'}),
  (q2:QA {name: '如何申请退换货？', question: '如何申请退换货？', answer: '在订单页面点击"申请退换货"，填写原因后提交即可。'}),
  (q3:QA {name: '保修期是多久？', question: '保修期是多久？', answer: '不同产品保修期不同：智能手表2年，蓝牙耳机1年，智能手机1年。'}),
  (q4:QA {name: '退款多久到账？', question: '退款多久到账？', answer: '审核通过后，支付宝/微信会在24小时内到账，银行卡需要3-5个工作日。'}),
  (q5:QA {name: '如何查询物流？', question: '如何查询物流？', answer: '在订单详情页点击"查看物流"即可查询实时物流信息。'}),
  (q6:QA {name: '可以开发票吗？', question: '可以开发票吗？', answer: '可以，请在确认收货后联系客服申请增值税普通发票或专用发票。'})
```

## 四、关系建立

```cypher
// 产品-服务关系
MATCH (p:Product {name: '智能手表'}), (s:Service {name: '两年质保'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '智能手表'}), (s:Service {name: '七天无理由退换'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '智能手表'}), (s:Service {name: '运费险'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '蓝牙耳机'}), (s:Service {name: '一年保修'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '蓝牙耳机'}), (s:Service {name: '七天无理由退换'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '智能手机'}), (s:Service {name: '一年保修'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '智能手机'}), (s:Service {name: '七天无理由退换'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '智能手机'}), (s:Service {name: '极速退款'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '平板电脑'}), (s:Service {name: '两年质保'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '平板电脑'}), (s:Service {name: '七天无理由退换'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '智能手环'}), (s:Service {name: '一年保修'})
CREATE (p)-[:HAS_SERVICE]->(s)

MATCH (p:Product {name: '智能手环'}), (s:Service {name: '七天无理由退换'})
CREATE (p)-[:HAS_SERVICE]->(s)
```

## 五、QA关联关系

```cypher
// QA与产品的关联
MATCH (q:QA {name: '如何申请保修？'}), (p:Product {name: '智能手表'})
CREATE (q)-[:REFERENCES]->(p)

MATCH (q:QA {name: '如何申请退换货？'}), (p:Product {name: '智能手表'})
CREATE (q)-[:REFERENCES]->(p)

MATCH (q:QA {name: '保修期是多久？'}), (p:Product {name: '智能手表'})
CREATE (q)-[:REFERENCES]->(p)

MATCH (q:QA {name: '保修期是多久？'}), (p:Product {name: '蓝牙耳机'})
CREATE (q)-[:REFERENCES]->(p)

MATCH (q:QA {name: '保修期是多久？'}), (p:Product {name: '智能手机'})
CREATE (q)-[:REFERENCES]->(p)

MATCH (q:QA {name: '退款多久到账？'}), (s:Service {name: '极速退款'})
CREATE (q)-[:RELATED_TO]->(s)

MATCH (q:QA {name: '如何申请退换货？'}), (s:Service {name: '七天无理由退换'})
CREATE (q)-[:RELATED_TO]->(s)

MATCH (q:QA {name: '如何申请保修？'}), (s:Service {name: '两年质保'})
CREATE (q)-[:RELATED_TO]->(s)

MATCH (q:QA {name: '如何申请保修？'}), (s:Service {name: '一年保修'})
CREATE (q)-[:RELATED_TO]->(s)

MATCH (q:QA {name: '可以开发票吗？'}), (p:Product {name: '智能手表'})
CREATE (q)-[:REFERENCES]->(p)
```

## 六、完整导入脚本（一键执行）

```cypher
// ============================================
// 知识图谱测试数据 - 一键导入
// 在 Neo4j Browser 中执行此脚本即可
// ============================================

// 1. 创建产品
CREATE
  (p1:Product {name: '智能手表', category: '电子产品', price: 2999}),
  (p2:Product {name: '蓝牙耳机', category: '电子产品', price: 899}),
  (p3:Product {name: '智能手机', category: '电子产品', price: 5999}),
  (p4:Product {name: '平板电脑', category: '电子产品', price: 3999}),
  (p5:Product {name: '智能手环', category: '电子产品', price: 399})

// 2. 创建服务
CREATE
  (s1:Service {name: '两年质保', type: 'warranty', description: '两年内非人为损坏免费维修'}),
  (s2:Service {name: '七天无理由退换', type: 'return', description: '收货7天内可申请退换货'}),
  (s3:Service {name: '一年保修', type: 'warranty', description: '一年内免费保修'}),
  (s4:Service {name: '运费险', type: 'shipping', description: '退货时补贴运费'}),
  (s5:Service {name: '极速退款', type: 'refund', description: '审核通过后24小时内退款'})

// 3. 创建QA
CREATE
  (q1:QA {name: '如何申请保修？', question: '如何申请保修？', answer: '请联系客服，提供订单号和产品问题描述，我们会在24小时内安排维修。'}),
  (q2:QA {name: '如何申请退换货？', question: '如何申请退换货？', answer: '在订单页面点击"申请退换货"，填写原因后提交即可。'}),
  (q3:QA {name: '保修期是多久？', question: '保修期是多久？', answer: '不同产品保修期不同：智能手表2年，蓝牙耳机1年，智能手机1年。'}),
  (q4:QA {name: '退款多久到账？', question: '退款多久到账？', answer: '审核通过后，支付宝/微信会在24小时内到账，银行卡需要3-5个工作日。'}),
  (q5:QA {name: '可以开发票吗？', question: '可以开发票吗？', answer: '可以，请在确认收货后联系客服申请发票。'})

// 4. 建立产品-服务关系
MATCH (p1:Product {name: '智能手表'}), (s1:Service {name: '两年质保'})
CREATE (p1)-[:HAS_SERVICE]->(s1)
MATCH (p1:Product {name: '智能手表'}), (s2:Service {name: '七天无理由退换'})
CREATE (p1)-[:HAS_SERVICE]->(s2)
MATCH (p1:Product {name: '智能手表'}), (s4:Service {name: '运费险'})
CREATE (p1)-[:HAS_SERVICE]->(s4)

MATCH (p2:Product {name: '蓝牙耳机'}), (s3:Service {name: '一年保修'})
CREATE (p2)-[:HAS_SERVICE]->(s3)
MATCH (p2:Product {name: '蓝牙耳机'}), (s2:Service {name: '七天无理由退换'})
CREATE (p2)-[:HAS_SERVICE]->(s2)

MATCH (p3:Product {name: '智能手机'}), (s3:Service {name: '一年保修'})
CREATE (p3)-[:HAS_SERVICE]->(s3)
MATCH (p3:Product {name: '智能手机'}), (s2:Service {name: '七天无理由退换'})
CREATE (p3)-[:HAS_SERVICE]->(s2)
MATCH (p3:Product {name: '智能手机'}), (s5:Service {name: '极速退款'})
CREATE (p3)-[:HAS_SERVICE]->(s5)

MATCH (p4:Product {name: '平板电脑'}), (s1:Service {name: '两年质保'})
CREATE (p4)-[:HAS_SERVICE]->(s1)
MATCH (p4:Product {name: '平板电脑'}), (s2:Service {name: '七天无理由退换'})
CREATE (p4)-[:HAS_SERVICE]->(s2)

MATCH (p5:Product {name: '智能手环'}), (s3:Service {name: '一年保修'})
CREATE (p5)-[:HAS_SERVICE]->(s3)
MATCH (p5:Product {name: '智能手环'}), (s2:Service {name: '七天无理由退换'})
CREATE (p5)-[:HAS_SERVICE]->(s2)

// 5. 建立QA关系
MATCH (q1:QA {name: '如何申请保修？'}), (p1:Product {name: '智能手表'})
CREATE (q1)-[:REFERENCES]->(p1)
MATCH (q1:QA {name: '如何申请保修？'}), (s1:Service {name: '两年质保'})
CREATE (q1)-[:RELATED_TO]->(s1)

MATCH (q2:QA {name: '如何申请退换货？'}), (p1:Product {name: '智能手表'})
CREATE (q2)-[:REFERENCES]->(p1)
MATCH (q2:QA {name: '如何申请退换货？'}), (s2:Service {name: '七天无理由退换'})
CREATE (q2)-[:RELATED_TO]->(s2)

MATCH (q3:QA {name: '保修期是多久？'}), (p1:Product {name: '智能手表'})
CREATE (q3)-[:REFERENCES]->(p1)
MATCH (q3:QA {name: '保修期是多久？'}), (p2:Product {name: '蓝牙耳机'})
CREATE (q3)-[:REFERENCES]->(p2)
MATCH (q3:QA {name: '保修期是多久？'}), (p3:Product {name: '智能手机'})
CREATE (q3)-[:REFERENCES]->(p3)

MATCH (q4:QA {name: '退款多久到账？'}), (s5:Service {name: '极速退款'})
CREATE (q4)-[:RELATED_TO]->(s5)

MATCH (q5:QA {name: '可以开发票吗？'}), (p1:Product {name: '智能手表'})
CREATE (q5)-[:REFERENCES]->(p1)
```

## 七、验证查询

```cypher
// 查看所有产品
MATCH (p:Product) RETURN p

// 查看所有服务
MATCH (s:Service) RETURN s

// 查看所有QA
MATCH (q:QA) RETURN q

// 查看图谱统计
MATCH (n) RETURN labels(n)[0] as type, count(n) as count

// 查看产品和服务的关系
MATCH (p:Product)-[r:HAS_SERVICE]->(s:Service) RETURN p.name, s.name

// 测试多跳查询：智能手表2跳内能到达的所有节点
MATCH path = (p:Product {name: '智能手表'})-[*1..2]-(connected)
RETURN path

// 测试QA的关联
MATCH (q:QA)-[r]-(connected) RETURN q.name, type(r), connected.name
```

## 八、预期结果

执行完整导入后，图谱结构如下：

```
                    ┌─────────────┐
                    │   智能手表   │ (Product)
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           │               │               │
     HAS_SERVICE      HAS_SERVICE     HAS_SERVICE
           │               │               │
           ▼               ▼               ▼
    ┌──────────┐    ┌──────────┐    ┌──────────┐
    │ 两年质保 │    │七天无理由 │    │  运费险   │
    │(Service) │    │   退换   │    │(Service) │
    └──────────┘    └──────────┘    └──────────┘
           │               │
     RELATED_TO        RELATED_TO
           │               │
           ▼               ▼
    ┌─────────────┐    ┌─────────────┐
    │如何申请保修？│   │如何申请退换货？│
    │    (QA)    │    │    (QA)     │
    └─────────────┘    └─────────────┘
           │
     REFERENCES
           │
           ▼
    ┌─────────────┐
    │   智能手表   │ ← 自引用（也可不加）
    │ (Product)   │
    └─────────────┘
```
