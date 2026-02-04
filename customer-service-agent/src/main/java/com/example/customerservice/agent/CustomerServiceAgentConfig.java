package com.example.customerservice.agent;

import com.example.customerservice.tools.CustomerServiceTools;
import com.example.customerservice.tools.KnowledgeBaseTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 客服Agent配置类
 * 负责初始化和配置客服Agent及其相关组件
 */
@Configuration
public class CustomerServiceAgentConfig {

    @Value("${agentscope.openai.api-key}")
    private String apiKey;

    @Value("${agentscope.openai.base-url}")
    private String baseUrl;

    @Value("${agentscope.agent.model.name:qwen3-max}")
    private String modelName;

    /**
     * 创建客服Agent的工具包
     * 注册所有客服相关的工具
     *
     * @return 配置好的工具包
     */
    @Bean
    public Toolkit customerServiceToolkit(KnowledgeBaseTools knowledgeBaseTools) {
        Toolkit toolkit = new Toolkit();
        // 注册客服工具
        toolkit.registerTool(new CustomerServiceTools());

        // 初始化并注册知识库工具
        toolkit.registerTool(knowledgeBaseTools);

        return toolkit;
    }

    /**
     * 创建DashScope聊天模型实例
     * 用于与阿里云百炼平台交互
     *
     * @return 配置好的聊天模型
     */
    @Bean
    public OpenAIChatModel openAIChatModel() {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    /**
     * 创建客服Agent实例
     * 配置系统提示词、模型和工具包
     *
     * @param chatModel 聊天模型
     * @param toolkit 工具包
     * @return 配置好的客服Agent
     */
    @Bean
    public ReActAgent customerServiceAgent(OpenAIChatModel chatModel, Toolkit toolkit) {
        String systemPrompt = """
                你是一个专业的智能客服助手，你的职责是为客户提供准确、及时的帮助。请遵循以下指导原则：

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
                """;

        return ReActAgent.builder()
                .name("智能客服")
                .sysPrompt(systemPrompt)
                .model(chatModel)
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .build();
    }
}
