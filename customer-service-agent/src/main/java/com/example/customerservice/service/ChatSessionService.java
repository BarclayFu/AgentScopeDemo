package com.example.customerservice.service;

import com.example.customerservice.tools.CustomerServiceTools;
import com.example.customerservice.tools.KnowledgeBaseTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天会话管理服务
 * 负责管理用户的会话状态，为每个用户创建和维护独立的Agent实例
 */
@Service
public class ChatSessionService {

    @Value("${agentscope.openai.api-key}")
    private String dashScopeApiKey;

    @Value("${agentscope.agent.model.name}")
    private String modelName;

    // 存储用户会话的映射表
    // 在实际生产环境中，这里应该使用Redis等外部存储
    private final Map<String, ReActAgent> userSessions = new ConcurrentHashMap<>();

    // 全局工具包
    private Toolkit globalToolkit;

    // 全局模型实例
    @Autowired
    private OpenAIChatModel globalModel;

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
        globalToolkit.registerTool(new CustomerServiceTools());

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
        String systemPrompt = """
                你是一个专业的智能客服助手，为用户ID为 %s 的客户提供服务。请遵循以下指导原则：

                1. 专业礼貌：始终保持专业的态度和礼貌的语调与客户交流。
                2. 准确回答：基于所提供的工具和信息准确回答客户问题。
                3. 主动服务：主动询问客户的需求，提供解决方案。
                4. 处理流程：
                   - 若客户咨询订单状态，请使用query_order_status工具
                   - 若客户需要办理退款，请使用process_refund工具
                   - 若客户询问产品信息，请使用query_product_info工具
                   - 若客户查询物流信息，请使用query_shipping_status工具
                   - 若客户询问常见问题，请使用search_knowledge_base工具
                5. 限制说明：只能处理与订单、产品、物流、退款、常见问题相关的咨询，其他问题请引导客户联系人工客服。

                请记住，客户满意度是我们的首要目标，请尽最大努力帮助每一位客户解决问题。
                """.formatted(userId);

        return ReActAgent.builder()
                .name("智能客服-" + userId)
                .sysPrompt(systemPrompt)
                .model(globalModel)
                .toolkit(globalToolkit)
                .memory(new InMemoryMemory())
                .build();
    }

    /**
     * 处理用户消息
     *
     * @param userId 用户ID
     * @param userMessage 用户消息内容
     * @return Agent回复
     */
    public Msg processUserMessage(String userId, String userMessage) {
        ReActAgent agent = getUserSession(userId);

        Msg userMsg = Msg.builder()
                .name("user-" + userId)
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userMessage).build())
                .build();

        // 调用Agent处理消息
        return agent.call(userMsg).block();
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
