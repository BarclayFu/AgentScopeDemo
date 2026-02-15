package com.example.customerservice.tools;

import com.example.customerservice.service.AgentActivityLogger;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 客服工具类，包含处理常见客服场景的工具方法
 * 这些工具将被Agent调用以处理客户请求
 */
@Component
public class CustomerServiceTools {

    private static final Logger logger = LoggerFactory.getLogger(
        CustomerServiceTools.class
    );

    @Autowired
    private AgentActivityLogger activityLogger;

    // 注入Knowledge Bean用于向量数据库操作
    private Knowledge knowledgeBase;

    // 模拟订单数据库
    private static final Map<String, Order> orderDatabase = new HashMap<>();

    // 初始化一些示例订单数据
    static {
        orderDatabase.put(
            "ORD001",
            new Order("ORD001", "iPhone 15 Pro", 999.99, "已发货", "2024-01-15")
        );
        orderDatabase.put(
            "ORD002",
            new Order(
                "ORD002",
                "MacBook Air M2",
                1199.99,
                "处理中",
                "2024-01-10"
            )
        );
        orderDatabase.put(
            "ORD003",
            new Order("ORD003", "AirPods Pro", 249.99, "已完成", "2024-01-05")
        );
    }

    /**
     * 设置Knowledge实例，用于向量数据库操作
     * @param knowledge Knowledge实例
    /**
     * 设置Knowledge实例，用于向量数据库操作
     * @param knowledge Knowledge实例
     */
    public void setKnowledgeBase(Knowledge knowledge) {
        this.knowledgeBase = knowledge;
        logger.info("KnowledgeBase已注入到CustomerServiceTools中");
    }

    /**
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    @Tool(name = "query_order_status", description = "查询订单状态和详情")
    public String queryOrderStatus(
        @ToolParam(
            name = "orderId",
            description = "订单ID，格式如ORD001"
        ) String orderId
    ) {
        activityLogger.logToolCallStart(
            "query_order_status",
            "orderId=" + orderId
        );
        logger.info("开始查询订单状态，订单ID: {}", orderId);
        Order order = orderDatabase.get(orderId);
        if (order != null) {
            String result = String.format(
                "订单ID: %s\n商品: %s\n价格: $%.2f\n状态: %s\n下单日期: %s",
                order.id(),
                order.productName(),
                order.price(),
                order.status(),
                order.orderDate()
            );
            logger.info(
                "订单查询成功，订单ID: {}, 状态: {}",
                orderId,
                order.status()
            );
            activityLogger.logToolCallEnd("query_order_status", result);
            return result;
        } else {
            String errorMessage = String.format(
                "未找到订单ID为 %s 的订单，请检查订单号是否正确。",
                orderId
            );
            logger.warn("订单查询失败，未找到订单ID: {}", orderId);
            activityLogger.logToolCallEnd("query_order_status", errorMessage);
            return errorMessage;
        }
    }

    /**
     * 处理退款请求工具
     *
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 退款处理结果
     */
    @Tool(name = "process_refund", description = "处理退款请求")
    public String processRefund(
        @ToolParam(name = "orderId", description = "订单ID") String orderId,
        @ToolParam(name = "reason", description = "退款原因") String reason
    ) {
        activityLogger.logToolCallStart(
            "process_refund",
            "orderId=" + orderId + ", reason=" + reason
        );
        logger.info(
            "开始处理退款请求，订单ID: {}, 退款原因: {}",
            orderId,
            reason
        );
        Order order = orderDatabase.get(orderId);
        if (order == null) {
            String errorMessage = String.format(
                "未找到订单ID为 %s 的订单，无法处理退款。",
                orderId
            );
            logger.warn("退款处理失败，未找到订单ID: {}", orderId);
            activityLogger.logToolCallEnd("process_refund", errorMessage);
            return errorMessage;
        }

        // 生成退款编号
        String refundId = "REF" + (System.currentTimeMillis() % 1000000);
        logger.info("生成退款编号: {}", refundId);

        // 模拟退款处理逻辑
        String result = String.format(
            "退款请求已受理\n退款编号: %s\n订单ID: %s\n商品: %s\n退款原因: %s\n" +
                "预计1-3个工作日内处理完成，请注意查收退款款项。",
            refundId,
            order.id(),
            order.productName(),
            reason
        );
        logger.info(
            "退款请求处理成功，订单ID: {}, 退款编号: {}",
            orderId,
            refundId
        );
        activityLogger.logToolCallEnd("process_refund", result);
        return result;
    }

    /**
     * 查询产品信息工具
     *
     * @param productName 产品名称
     * @return 产品信息
     */
    @Tool(name = "query_product_info", description = "查询产品详细信息")
    public String queryProductInfo(
        @ToolParam(
            name = "productName",
            description = "产品名称"
        ) String productName
    ) {
        activityLogger.logToolCallStart(
            "query_product_info",
            "productName=" + productName
        );
        logger.info("开始查询产品信息，产品名称: {}", productName);
        // 模拟产品数据库查询
        Map<String, String> productInfo = new HashMap<>();
        productInfo.put(
            "iPhone 15 Pro",
            "iPhone 15 Pro搭载A17 Pro芯片，配备超瓷晶面板，支持5G网络，后置三摄系统。"
        );
        productInfo.put(
            "MacBook Air M2",
            "MacBook Air M2采用苹果M2芯片，13.6英寸 Liquid Retina 显示屏，轻薄便携。"
        );
        productInfo.put(
            "AirPods Pro",
            "AirPods Pro主动降噪耳机，支持空间音频，自适应通透模式。"
        );

        String info = productInfo.get(productName);
        if (info != null) {
            String result = String.format(
                "产品名称: %s\n产品信息: %s",
                productName,
                info
            );
            logger.info("产品信息查询成功，产品名称: {}", productName);
            activityLogger.logToolCallEnd("query_product_info", result);
            return result;
        } else {
            String errorMessage = String.format(
                "抱歉，未找到产品 %s 的详细信息。",
                productName
            );
            logger.warn("产品信息查询失败，未找到产品: {}", productName);
            activityLogger.logToolCallEnd("query_product_info", errorMessage);
            return errorMessage;
        }
    }

    /**
     * 查询物流信息工具
     *
     * @param orderId 订单ID
     * @return 物流信息
     */
    @Tool(name = "query_shipping_status", description = "查询订单物流状态")
    public String queryShippingStatus(
        @ToolParam(name = "orderId", description = "订单ID") String orderId
    ) {
        activityLogger.logToolCallStart(
            "query_shipping_status",
            "orderId=" + orderId
        );
        logger.info("开始查询物流状态，订单ID: {}", orderId);
        Order order = orderDatabase.get(orderId);
        if (order == null) {
            String errorMessage = String.format(
                "未找到订单ID为 %s 的订单，无法查询物流信息。",
                orderId
            );
            logger.warn("物流状态查询失败，未找到订单ID: {}", orderId);
            activityLogger.logToolCallEnd(
                "query_shipping_status",
                errorMessage
            );
            return errorMessage;
        }

        // 模拟物流信息
        String[] shippingStatuses = {
            "已发货，预计1-2天送达",
            "运输中，已到达配送中心",
            "正在派送中，请保持电话畅通",
            "已送达",
        };

        Random random = new Random();
        String status = shippingStatuses[random.nextInt(
            shippingStatuses.length
        )];
        String currentTime = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );

        logger.info("物流状态查询成功，订单ID: {}, 状态: {}", orderId, status);

        String result = String.format(
            "订单 %s 的物流状态: %s\n更新时间: %s",
            orderId,
            status,
            currentTime
        );
        activityLogger.logToolCallEnd("query_shipping_status", result);
        return result;
    }

    /**
     * 订单记录类
     */
    public record Order(
        String id,
        String productName,
        double price,
        String status,
        String orderDate
    ) {}

    /**
     * 添加知识到向量数据库工具
     *
     * @param title 知识标题
     * @param content 知识内容
     * @return 添加结果
     */
    @Tool(name = "add_knowledge", description = "向知识库中添加新的客服知识")
    public String addKnowledge(
        @ToolParam(name = "title", description = "知识标题") String title,
        @ToolParam(name = "content", description = "知识内容") String content
    ) {
        activityLogger.logToolCallStart("add_knowledge", "title=" + title);
        logger.info("开始添加知识到向量数据库，标题: {}", title);
        if (knowledgeBase == null) {
            logger.error("知识库未初始化");
            activityLogger.logToolCallEnd(
                "add_knowledge",
                "错误：知识库未初始化"
            );
            return "错误：知识库未初始化";
        }

        try {
            logger.debug(
                "使用TextReader创建文档，内容长度: {}",
                content.length()
            );
            // 使用TextReader创建文档
            TextReader reader = new TextReader();
            ReaderInput input = ReaderInput.fromString(content);
            List<Document> documents = reader.read(input).block();

            if (documents != null && !documents.isEmpty()) {
                logger.info(
                    "成功创建{}个文档，开始添加到知识库",
                    documents.size()
                );
                // 添加到知识库
                knowledgeBase.addDocuments(documents).block();
                logger.info("成功添加知识到向量数据库，标题: {}", title);

                String result = String.format(
                    "成功添加知识到向量数据库\n标题: %s\n内容: %s",
                    title,
                    content
                );
                activityLogger.logToolCallEnd("add_knowledge", result);
                return result;
            } else {
                logger.warn("创建文档失败：未生成有效文档");
                activityLogger.logToolCallEnd(
                    "add_knowledge",
                    "创建文档失败：未生成有效文档"
                );
                return "创建文档失败：未生成有效文档";
            }
        } catch (Exception e) {
            logger.error("添加知识失败", e);
            String errorMessage = String.format(
                "添加知识失败: %s",
                e.getMessage()
            );
            activityLogger.logToolCallError("add_knowledge", errorMessage);
            return errorMessage;
        }
    }

    /**
     * 从向量数据库检索知识工具
     *
     * @param query 查询内容
     * @return 检索结果
     */
    @Tool(
        name = "retrieve_knowledge",
        description = "从知识库中检索相关客服知识"
    )
    public String retrieveKnowledge(
        @ToolParam(name = "query", description = "查询内容") String query
    ) {
        activityLogger.logToolCallStart("retrieve_knowledge", "query=" + query);
        logger.info("开始从向量数据库检索知识，查询内容: {}", query);
        if (knowledgeBase == null) {
            logger.error("知识库未初始化");
            String errorMessage = "错误：知识库未初始化";
            activityLogger.logToolCallEnd("retrieve_knowledge", errorMessage);
            return errorMessage;
        }

        try {
            // 创建检索配置
            RetrieveConfig config = RetrieveConfig.builder().limit(3).build();
            logger.debug("创建检索配置，限制返回结果数: {}", config.getLimit());

            // 从知识库检索相关文档
            List<Document> results = knowledgeBase
                .retrieve(query, config)
                .block();

            if (results == null || results.isEmpty()) {
                logger.info("未找到相关知识，查询内容: {}", query);
                String result = "未找到相关知识";
                activityLogger.logToolCallEnd("retrieve_knowledge", result);
                return result;
            }

            logger.info("成功检索到{}个相关文档", results.size());

            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append("检索到以下相关知识:\n\n");

            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                DocumentMetadata metadata = doc.getMetadata();
                ContentBlock contentBlock = metadata.getContent();

                resultBuilder.append(
                    String.format("[%d] 相似度: %.2f\n", i + 1, doc.getScore())
                );
                if (contentBlock instanceof TextBlock) {
                    resultBuilder
                        .append(((TextBlock) contentBlock).getText())
                        .append("\n\n");
                }
            }

            String result = resultBuilder.toString();
            logger.debug("返回检索结果，结果长度: {}", result.length());
            activityLogger.logToolCallEnd("retrieve_knowledge", result);
            return result;
        } catch (Exception e) {
            logger.error("检索知识失败", e);
            String errorMessage = String.format(
                "检索知识失败: %s",
                e.getMessage()
            );
            activityLogger.logToolCallError("retrieve_knowledge", errorMessage);
            return errorMessage;
        }
    }
}
