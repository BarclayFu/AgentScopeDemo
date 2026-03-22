package com.example.customerservice.service.extractor;

import com.example.customerservice.service.KnowledgeGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 三元组抽取器
 *
 * 负责从知识条目中抽取知识图谱三元组（实体-关系-实体）。
 * 采用混合抽取策略：
 * 1. 规则抽取：基于正则表达式快速提取结构化信息（产品、订单、服务、Q&A）
 * 2. LLM抽取：使用大语言模型进行深层语义抽取
 *
 * 抽取结果直接存储到Neo4j图数据库中。
 */
@Component
public class TripleExtractor {

    private static final Logger logger = LoggerFactory.getLogger(TripleExtractor.class);

    /** 规则预处理器，用于快速提取结构化信息 */
    private final RulePreprocessor rulePreprocessor;

    /** LLM三元组抽取器，用于深层语义抽取 */
    private final LLMTripleExtractor llmTripleExtractor;

    /** 知识图谱服务，用于存储抽取的三元组 */
    private final KnowledgeGraphService knowledgeGraphService;

    public TripleExtractor(RulePreprocessor rulePreprocessor, LLMTripleExtractor llmTripleExtractor, KnowledgeGraphService knowledgeGraphService) {
        this.rulePreprocessor = rulePreprocessor;
        this.llmTripleExtractor = llmTripleExtractor;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * 从知识条目中抽取三元组并存储到图数据库
     *
     * 整个处理流程：
     * 1. 规则预提取：从文本中快速提取产品、服务、订单、Q&A等结构化信息
     * 2. 转换为三元组：将预提取的信息转换为(实体, 关系, 实体)格式
     * 3. LLM深层抽取：使用大语言模型从文本中抽取更多隐含的三元组
     * 4. 推断实体类型：根据抽取结果推断实体的类型（Product, Service, Order, QA, Concept）
     * 5. 存储到Neo4j：将三元组存储到图数据库
     *
     * @param knowledgeEntryId 知识条目ID，用于日志追踪
     * @param title           知识条目标题
     * @param content         知识条目内容
     */
    public void extractAndStore(String knowledgeEntryId, String title, String content) {
        String fullText = title + "\n" + content;
        List<Map<String, String>> triples = new ArrayList<>();

        // ========== 第一步：规则预提取 ==========
        // 使用正则表达式快速从文本中提取结构化信息
        Map<String, Object> preprocessed = rulePreprocessor.preprocess(fullText);

        // ========== 第二步：将预提取的信息转换为三元组 ==========

        // 2.1 提取产品信息 -> (产品, MENTIONS, 标题)
        @SuppressWarnings("unchecked")
        List<String> products = (List<String>) preprocessed.getOrDefault("products", List.of());
        for (String product : products) {
            triples.add(Map.of("subject", product, "relation", "MENTIONS", "object", title));
        }

        // 2.2 提取服务信息 -> (标题, HAS_SERVICE, 服务)
        @SuppressWarnings("unchecked")
        List<String> services = (List<String>) preprocessed.getOrDefault("services", List.of());
        for (String service : services) {
            triples.add(Map.of("subject", title, "relation", "HAS_SERVICE", "object", service));
        }

        // 2.3 提取问答对 -> (问题, RELATED_TO, 答案)
        @SuppressWarnings("unchecked")
        List<Map<String, String>> qas = (List<Map<String, String>>) preprocessed.getOrDefault("qas", List.of());
        for (Map<String, String> qa : qas) {
            triples.add(Map.of(
                "subject", qa.get("question"),
                "relation", "RELATED_TO",
                "object", qa.get("answer")
            ));
        }

        // ========== 第三步：LLM深层抽取 ==========
        // 使用大语言模型从文本中抽取更多隐含的语义关系
        try {
            List<Map<String, String>> llmTriples = llmTripleExtractor.extractTriples(fullText);
            triples.addAll(llmTriples);
        } catch (Exception e) {
            logger.warn("LLM抽取失败 entry {}: {}", knowledgeEntryId, e.getMessage());
        }

        // ========== 第四步和第五步：推断类型并存储 ==========
        for (Map<String, String> triple : triples) {
            String subject = triple.get("subject");
            String relation = triple.get("relation");
            String object = triple.get("object");

            // 推断主语和宾语的实体类型
            String subjectType = inferEntityType(subject, preprocessed);
            String objectType = inferEntityType(object, preprocessed);

            // 只有当实体类型都能确定时才存储
            if (subjectType != null && objectType != null) {
                try {
                    knowledgeGraphService.addTriple(subject, subjectType, relation, object, objectType);
                    logger.debug("已存储三元组: ({})-[:{}]->({})", subject, relation, object);
                } catch (Exception e) {
                    logger.warn("存储三元组失败: ({})-[:{}]->({})", subject, relation, object);
                }
            }
        }

        logger.info("知识条目 {} 抽取并存储了 {} 个三元组", knowledgeEntryId, triples.size());
    }

    /**
     * 推断实体类型
     *
     * 根据预提取的信息和实体名称的特征来推断实体的类型。
     *
     * 推断规则：
     * - 如果实体在预提取的产品列表中 -> Product
     * - 如果实体在预提取的服务列表中 -> Service
     * - 如果实体在预提取的订单列表中 -> Order
     * - 如果实体包含问号或疑问词(如何、怎么) -> QA
     * - 其他情况 -> Concept（通用概念）
     *
     * @param entity       实体名称
     * @param preprocessed 预提取的结构化信息
     * @return 实体类型，如果无法推断则返回null
     */
    @SuppressWarnings("unchecked")
    private String inferEntityType(String entity, Map<String, Object> preprocessed) {
        List<String> products = (List<String>) preprocessed.getOrDefault("products", List.of());
        List<String> services = (List<String>) preprocessed.getOrDefault("services", List.of());
        List<String> orders = (List<String>) preprocessed.getOrDefault("orders", List.of());

        if (products.contains(entity)) return "Product";
        if (services.contains(entity)) return "Service";
        if (orders.contains(entity)) return "Order";
        // 问答类实体通常包含问号或疑问词
        if (entity.contains("?") || entity.contains("如何") || entity.contains("怎么")) return "QA";
        // 默认归类为通用概念
        return "Concept";
    }
}