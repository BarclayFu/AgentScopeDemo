package com.example.customerservice.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

import com.example.customerservice.service.KnowledgeBaseService;

/**
 * 知识库工具类，包含知识库搜索功能
 * 这些工具将被Agent调用以检索相关知识
 */
@Component
public class KnowledgeBaseTools {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseTools(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 根据问题检索相关文档
     *
     * @param question 用户问题
     * @return 相关文档内容
     */
    @Tool(name = "search_knowledge_base", description = "在知识库中搜索与问题相关的信息")
    public String searchKnowledgeBase(
            @ToolParam(name = "question", description = "用户提出的问题") String question) {
        return knowledgeBaseService.searchKnowledgeBase(question);
    }
}
