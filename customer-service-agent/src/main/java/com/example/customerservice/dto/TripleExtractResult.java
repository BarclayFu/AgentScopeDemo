package com.example.customerservice.dto;

import java.util.List;
import java.util.Map;

/**
 * 三元组抽取预览结果
 *
 * 包含规则抽取和LLM抽取的结果，用于预览而不存储。
 */
public class TripleExtractResult {
    /** 规则抽取的三元组列表 */
    private final List<Map<String, String>> ruleTriples;

    /** LLM抽取的三元组列表 */
    private final List<Map<String, String>> llmTriples;

    /** 预处理提取的实体信息 */
    private final Map<String, Object> preprocessedEntities;

    public TripleExtractResult(
            List<Map<String, String>> ruleTriples,
            List<Map<String, String>> llmTriples,
            Map<String, Object> preprocessedEntities) {
        this.ruleTriples = ruleTriples;
        this.llmTriples = llmTriples;
        this.preprocessedEntities = preprocessedEntities;
    }

    public List<Map<String, String>> getRuleTriples() { return ruleTriples; }
    public List<Map<String, String>> getLlmTriples() { return llmTriples; }
    public Map<String, Object> getPreprocessedEntities() { return preprocessedEntities; }

    /** 获取合并后的所有三元组数量 */
    public int getTotalCount() {
        return ruleTriples.size() + llmTriples.size();
    }
}