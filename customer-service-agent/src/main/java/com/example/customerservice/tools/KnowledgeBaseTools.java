package com.example.customerservice.tools;

import com.example.customerservice.service.AgentActivityLogger;
import com.example.customerservice.service.KnowledgeBaseService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 知识库工具类，包含知识库搜索功能
 * 这些工具将被Agent调用以检索相关知识
 */
@Component
public class KnowledgeBaseTools {

    private static final Logger logger = LoggerFactory.getLogger(
        KnowledgeBaseTools.class
    );

    private final KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private AgentActivityLogger activityLogger;

    public KnowledgeBaseTools(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 根据问题检索相关文档
     *
     * @param question 用户问题
     * @return 相关文档内容
     */
    @Tool(
        name = "search_knowledge_base",
        description = "在知识库中搜索与问题相关的信息"
    )
    public String searchKnowledgeBase(
        @ToolParam(
            name = "question",
            description = "用户提出的问题"
        ) String question
    ) {
        activityLogger.logToolCallStart(
            "search_knowledge_base",
            "question=" + question
        );
        logger.info("开始搜索知识库，问题: {}", question);
        try {
            String result = knowledgeBaseService.searchKnowledgeBase(question);
            logger.info(
                "知识库搜索完成，结果长度: {}",
                result != null ? result.length() : 0
            );
            activityLogger.logToolCallEnd("search_knowledge_base", result);
            return result;
        } catch (Exception e) {
            activityLogger.logToolCallError(
                "search_knowledge_base",
                e.getMessage()
            );
            throw e;
        }
    }
}
