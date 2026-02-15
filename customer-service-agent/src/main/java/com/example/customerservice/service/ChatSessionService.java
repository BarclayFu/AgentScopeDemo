package com.example.customerservice.service;

import com.example.customerservice.tools.CustomerServiceTools;
import com.example.customerservice.tools.KnowledgeBaseTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 聊天会话管理服务
 * 负责管理用户的会话状态，为每个用户创建和维护独立的Agent实例
 */
@Service
public class ChatSessionService {

    private static final Logger logger = LoggerFactory.getLogger(
        ChatSessionService.class
    );
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile(
        "\\b(ORD\\d{3,})\\b",
        Pattern.CASE_INSENSITIVE
    );

    @Autowired
    private AgentActivityLogger activityLogger;

    @Value("${agentscope.openai.api-key}")
    private String dashScopeApiKey;

    @Value("${agentscope.agent.model.name}")
    private String modelName;

    // 存储用户会话的映射表
    // 在实际生产环境中，这里应该使用Redis等外部存储
    private final Map<String, ReActAgent> userSessions =
        new ConcurrentHashMap<>();

    // 全局工具包
    private Toolkit globalToolkit;

    // 全局模型实例
    @Autowired
    private OpenAIChatModel globalModel;

    @Autowired
    private CustomerServiceTools customerServiceTools;

    private final KnowledgeBaseTools knowledgeBaseTools;

    public ChatSessionService(KnowledgeBaseTools knowledgeBaseTools) {
        this.knowledgeBaseTools = knowledgeBaseTools;
    }

    /**
     * 初始化全局组件
     * 在Spring容器启动后自动执行
     */
    @PostConstruct
    public void init() {
        // 初始化工具包
        globalToolkit = new Toolkit();
        // 使用Spring注入的CustomerServiceTools实例
        globalToolkit.registerTool(customerServiceTools);

        // 初始化并注册知识库工具
        globalToolkit.registerTool(knowledgeBaseTools);
    }

    /**
     * 获取或创建用户会话
     * 为每个用户维护独立的Agent实例和对话历史
     *
     * @param userId 用户ID
     * @return 用户的Agent实例
     */
    public ReActAgent getUserSession(String userId) {
        return userSessions.computeIfAbsent(userId, this::createUserAgent);
    }

    /**
     * 创建用户专属的Agent实例
     *
     * @param userId 用户ID
     * @return 新创建的Agent实例
     */
    private ReActAgent createUserAgent(String userId) {
        logger.info("为用户 {} 创建新的客服Agent实例", userId);

        String systemPrompt = """
            你是一个专业的智能客服助手，为用户ID为 %s 的客户提供服务。请遵循以下指导原则：

            1. 专业礼貌：始终保持专业的态度和礼貌的语调与客户交流。
            2. 准确回答：基于所提供的工具和信息准确回答客户问题。
            3. 主动服务：主动询问客户的需求，提供解决方案。
            4. 处理流程：
               - 若客户咨询订单状态，请立即使用query_order_status工具
               - 若客户需要办理退款，请立即使用process_refund工具
               - 若客户询问产品信息，请立即使用query_product_info工具
               - 若客户查询物流信息，请立即使用query_shipping_status工具
               - 若客户询问常见问题，请立即使用search_knowledge_base工具
            5. 重要说明：识别到客户需求后应立即调用相应工具，不要等待客户再次询问。
            6. 限制说明：只能处理与订单、产品、物流、退款、常见问题相关的咨询，其他问题请引导客户联系人工客服。
            7. 客观性要求：对于知识库存在的事实类问题，使用原文回答而不是编造回答，这可能严重涉及到法律问题，所以务必注意。
            请记住，客户满意度是我们的首要目标，请尽最大努力帮助每一位客户解决问题。
            """.formatted(userId);
        activityLogger.logMessageProcessingStart(
            "智能客服-" + userId,
            "创建新的客服会话"
        );
        ReActAgent agent = ReActAgent.builder()
            .name("智能客服-" + userId)
            .sysPrompt(systemPrompt)
            .model(globalModel)
            .toolkit(globalToolkit)
            .memory(new InMemoryMemory())
            // 开启元工具模式，兼容不稳定的函数调用模型，提升工具触发成功率
            .enableMetaTool(true)
            .toolExecutionConfig(io.agentscope.core.model.ExecutionConfig.builder().timeout(java.time.Duration.ofSeconds(30)).maxAttempts(1).build()) // 优化工具执行配置
            .maxIters(3) // 限制最大迭代次数
            .build();
        logger.info("用户 {} 的客服Agent实例创建完成", userId);
        return agent;
    }

    /**
     * 处理用户消息
     *
     * @param userId 用户ID
     * @param userMessage 用户消息内容
     * @return Agent回复
     */
    public Msg processUserMessage(String userId, String userMessage) {
        logger.info("处理用户 {} 的消息: {}", userId, userMessage);
        activityLogger.logMessageProcessingStart(
            "智能客服-" + userId,
            userMessage
        );

        if (isProductInfoIntent(userMessage)) {
            Msg directResponse = handleDirectProductQuery(userId, userMessage);
            logger.info(
                "用户 {} 的消息处理完成（产品信息直出），响应长度: {}, 内容为:{}",
                userId,
                directResponse.getTextContent().length(),
                directResponse.getContent()
            );
            activityLogger.logMessageProcessingEnd(
                "智能客服-" + userId,
                directResponse.getTextContent()
            );
            return directResponse;
        }

        if (isKnowledgeIntent(userMessage)) {
            Msg directResponse = handleDirectKnowledgeQuery(userId, userMessage);
            logger.info(
                "用户 {} 的消息处理完成（知识库直出），响应长度: {}, 内容为:{}",
                userId,
                directResponse.getTextContent().length(),
                directResponse.getContent()
            );
            activityLogger.logMessageProcessingEnd(
                "智能客服-" + userId,
                directResponse.getTextContent()
            );
            return directResponse;
        }

        ReActAgent agent = getUserSession(userId);

        Msg userMsg = Msg.builder()
            .name("user-" + userId)
            .role(MsgRole.USER)
            .content(TextBlock.builder().text(userMessage).build())
            .build();

        // 调用Agent处理消息
        Msg response = agent.call(userMsg).block();
        response = fallbackIfOrderStatusPending(userId, userMessage, response);
        response = fallbackIfKnowledgeQueryPending(userId, userMessage, response);

        logger.info(
            "用户 {} 的消息处理完成，响应长度: {}, 内容为:{}",
            userId,
            response != null ? response.getTextContent().length() : 0,
            response.getContent()
        );
        activityLogger.logMessageProcessingEnd(
            "智能客服-" + userId,
            response != null ? response.getTextContent() : ""
        );

        return response;
    }

    private Msg handleDirectProductQuery(String userId, String userMessage) {
        String productName = extractProductName(userMessage);
        String result = productName != null
            ? customerServiceTools.queryProductInfo(productName)
            : "抱歉，我暂时无法识别您咨询的产品名称。请提供更完整的产品名，例如 iPhone 15 Pro。";
        return Msg.builder()
            .name("智能客服-" + userId)
            .role(MsgRole.ASSISTANT)
            .content(TextBlock.builder().text(result).build())
            .build();
    }

    private Msg handleDirectKnowledgeQuery(String userId, String userMessage) {
        logger.info("命中知识库问题直出策略，userId={}, question={}", userId, userMessage);
        String toolResult = knowledgeBaseTools.searchKnowledgeBase(userMessage);
        String finalText = toolResult;
        return Msg.builder()
            .name("智能客服-" + userId)
            .role(MsgRole.ASSISTANT)
            .content(TextBlock.builder().text(finalText).build())
            .build();
    }

    private Msg fallbackIfOrderStatusPending(
        String userId,
        String userMessage,
        Msg response
    ) {
        String orderId = extractOrderId(userMessage);
        if (!isOrderStatusIntent(userMessage) || orderId == null) {
            return response;
        }

        String text = response != null ? response.getTextContent() : "";
        if (!looksLikePendingResponse(text)) {
            return response;
        }

        logger.warn(
            "检测到订单查询响应未给出结论，触发兜底工具调用。userId={}, orderId={}, response={}",
            userId,
            orderId,
            text
        );
        String toolResult = customerServiceTools.queryOrderStatus(orderId);
        String finalText = "已为您查询到订单状态：\n" + toolResult;
        return Msg.builder()
            .name("智能客服-" + userId)
            .role(MsgRole.ASSISTANT)
            .content(TextBlock.builder().text(finalText).build())
            .build();
    }

    private boolean isOrderStatusIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return message.contains("订单") &&
        (
            message.contains("状态") ||
            message.contains("查询") ||
            message.contains("查一下")
        );
    }

    private String extractOrderId(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Matcher matcher = ORDER_ID_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).toUpperCase();
    }

    private boolean isProductInfoIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        boolean asksFeature =
            message.contains("特性") ||
            message.contains("特点") ||
            message.contains("参数") ||
            message.contains("配置") ||
            message.contains("介绍") ||
            message.contains("信息") ||
            message.contains("有什么");
        return asksFeature && extractProductName(message) != null;
    }

    private String extractProductName(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String lower = message.toLowerCase();
        if (lower.contains("iphone 15 pro") || (lower.contains("iphone") && lower.contains("15"))) {
            return "iPhone 15 Pro";
        }
        if (lower.contains("macbook air m2") || (lower.contains("macbook") && lower.contains("m2"))) {
            return "MacBook Air M2";
        }
        if (lower.contains("airpods pro") || lower.contains("airpods")) {
            return "AirPods Pro";
        }
        return null;
    }

    private boolean looksLikePendingResponse(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }

        boolean hasPendingWord =
            text.contains("请稍候") ||
            text.contains("立刻为您查询") ||
            text.contains("马上为您查询") ||
            text.contains("稍等") ||
            text.contains("正在为您查询");
        boolean hasFinalOrderInfo =
            text.contains("订单ID") || text.contains("状态:") || text.contains("未找到订单");
        return hasPendingWord && !hasFinalOrderInfo;
    }

    private Msg fallbackIfKnowledgeQueryPending(
        String userId,
        String userMessage,
        Msg response
    ) {
        if (!isKnowledgeIntent(userMessage)) {
            return response;
        }

        String text = response != null ? response.getTextContent() : "";
        if (!looksLikeKnowledgePendingResponse(text)) {
            logger.info(
                "知识库兜底未触发。userId={}, response={}",
                userId,
                text
            );
            return response;
        }

        logger.warn(
            "检测到知识库查询响应未给出结论，触发兜底工具调用。userId={}, question={}, response={}",
            userId,
            userMessage,
            text
        );
        String toolResult = knowledgeBaseTools.searchKnowledgeBase(userMessage);
        String finalText = "为您查询到以下售后相关信息：\n" + toolResult;
        return Msg.builder()
            .name("智能客服-" + userId)
            .role(MsgRole.ASSISTANT)
            .content(TextBlock.builder().text(finalText).build())
            .build();
    }

    private boolean isKnowledgeIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        boolean policyLike =
            message.contains("售后") ||
            message.contains("政策") ||
            message.contains("规则") ||
            message.contains("保修") ||
            message.contains("人工客服") ||
            message.contains("退换货");
        boolean businessLike =
            message.contains("订单") ||
            message.contains("物流") ||
            message.contains("退款") ||
            message.contains("商品") ||
            extractOrderId(message) != null;
        return policyLike && !businessLike;
    }

    private boolean looksLikeKnowledgePendingResponse(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }

        boolean hasToolCallHint =
            text.contains("search_knowledge_base") || text.contains("调用工具");
        boolean hasPendingWord =
            text.contains("稍候") ||
            text.contains("稍等") ||
            text.contains("请稍候") ||
            text.contains("请您稍候") ||
            text.contains("正在为您查询") ||
            text.contains("正在查询") ||
            text.contains("马上为您查询") ||
            text.contains("我正在为您查询") ||
            text.contains("这就为您查询") ||
            text.contains("为您查询常见问题库");
        boolean hasFinalHint =
            text.contains("根据知识库") ||
            text.contains("处理时效") ||
            text.contains("联系人工客服") ||
            text.contains("支持") ||
            text.contains("为您查询到以下");
        return (hasToolCallHint || hasPendingWord) && !hasFinalHint;
    }

    /**
     * 清除用户会话（用于测试或会话重置）
     *
     * @param userId 用户ID
     */
    public void clearUserSession(String userId) {
        userSessions.remove(userId);
    }

    /**
     * 获取当前活动的会话数量
     *
     * @return 会话数量
     */
    public int getActiveSessionCount() {
        return userSessions.size();
    }
}
