package com.example.customerservice.service;

import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 知识库服务
 * 提供基于RAG的文档检索和问答功能
 */
@Service
public class KnowledgeBaseService {

    private final Knowledge knowledgeBase;

    public KnowledgeBaseService(Knowledge knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 初始化知识库
     */
    @PostConstruct
    public void init() {
        try {
            // 初始化一些示例文档到知识库
            initializeKnowledgeBase();
        } catch (Exception e) {
            System.err.println("知识库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化知识库内容
     */
    private void initializeKnowledgeBase() {
        try {
            // 客服常见问题和答案
            String faqContent = """
                问：如何查询订单状态？
                答：您可以通过以下方式查询订单状态：
                1. 登录官网个人账户，在"我的订单"页面查看
                2. 通过本智能客服，提供订单号即可查询
                3. 拨打客服热线400-XXX-XXXX，提供订单号查询

                问：如何办理退款？
                答：退款流程如下：
                1. 登录官网个人账户，进入订单详情页申请退款
                2. 通过本智能客服，提供订单号和退款原因办理
                3. 退款会在1-3个工作日内处理完成，款项原路返回

                问：发货后多久能收到商品？
                答：发货后到达时间因地区而异：
                1. 同城配送：1-2天
                2. 省内配送：2-4天
                3. 跨省配送：3-7天
                4. 特殊地区（西藏、新疆等）：5-10天
                5. 具体物流信息可在订单详情页查看

                问：如何联系人工客服？
                答：如需联系人工客服，请按以下方式操作：
                1. 拨打客服热线400-XXX-XXXX（工作时间：9:00-21:00）
                2. 在官网页面点击"联系客服"，选择"转人工"
                3. 通过本智能客服输入"转人工"申请转接
                """;

            // 产品使用指南
            String productGuideContent = """
                产品使用指南

                iPhone 15 Pro使用注意事项：
                1. 首次使用请使用原装充电器充电至100%
                2. 避免在温度过高或过低的环境中使用
                3. 定期清理充电口和扬声器网孔
                4. 不要使用尖锐物品触碰屏幕

                MacBook Air M2使用注意事项：
                1. 首次使用请充满电后再使用
                2. 避免液体溅到键盘和屏幕上
                3. 不要阻塞散热口
                4. 定期清理系统垃圾和缓存文件

                AirPods Pro使用注意事项：
                1. 佩戴时请确保耳塞贴合耳道
                2. 使用后及时放回充电盒
                3. 定期清洁耳塞和充电盒金属触点
                4. 避免在潮湿环境中使用
                """;

            // 售后服务政策
            String afterSalesPolicyContent = """
                售后服务政策

                退换货政策：
                1. 自签收之日起7天内可无理由退货（特殊商品除外）
                2. 15天内出现质量问题可换货
                3. 退货商品需保持原包装完整，配件齐全
                4. 退货产生的运费由客户承担（质量问题除外）

                保修政策：
                1. iPhone整机保修1年
                2. MacBook整机保修2年
                3. AirPods整机保修1年
                4. 保修期内非人为损坏免费维修

                维修服务：
                1. 官方授权维修点提供维修服务
                2. 维修周期一般为1-2周
                3. 贵重物品建议提前备份数据
                4. 维修前可先通过智能客服查询常见问题
                """;

            // 将文档添加到知识库
            addDocumentToKnowledgeBase("常见问题与解答", faqContent);
            addDocumentToKnowledgeBase("产品使用指南", productGuideContent);
            addDocumentToKnowledgeBase("售后服务政策", afterSalesPolicyContent);

            System.out.println("知识库初始化完成，共添加了3个文档");
        } catch (Exception e) {
            System.err.println("知识库文档初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 将文档添加到知识库
     *
     * @param title  文档标题
     * @param content 文档内容
     */
    private void addDocumentToKnowledgeBase(String title, String content) {
        try {
            TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
            List<Document> docs = reader.read(ReaderInput.fromString(content)).block();
            if (docs == null || docs.isEmpty()) {
                return;
            }

            List<Document> enrichedDocs = docs.stream()
                .map(doc -> {
                    DocumentMetadata meta = doc.getMetadata();
                    DocumentMetadata enriched = DocumentMetadata.builder()
                        .content(meta.getContent())
                        .docId(meta.getDocId())
                        .chunkId(meta.getChunkId())
                        .payload(Map.of("source", "customer-service-faq", "type", "faq", "title", title))
                        .build();
                    return new Document(enriched);
                })
                .toList();

            knowledgeBase.addDocuments(enrichedDocs).block();
        } catch (Exception e) {
            System.err.println("添加文档到知识库失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据问题检索相关文档
     *
     * @param question 用户问题
     * @return 相关文档内容
     */
    public String searchKnowledgeBase(String question) {
        try {
            // 配置检索参数
            RetrieveConfig config = RetrieveConfig.builder()
                .limit(3) // 返回最多3个相关文档
                .scoreThreshold(0.3) // 相似度阈值
                .build();

            // 执行检索
            List<Document> results = knowledgeBase
                .retrieve(question, config)
                .block();

            if (results == null || results.isEmpty()) {
                return "抱歉，知识库中没有找到与您的问题相关的信息。请尝试重新表述问题或联系人工客服。";
            }

            // 整合检索结果
            StringBuilder response = new StringBuilder();
            response.append("根据知识库中的信息，为您找到以下相关内容：\n\n");

            for (int i = 0; i < results.size() && i < 3; i++) {
                Document doc = results.get(i);
                String title = doc.getPayloadValueAs("title", String.class);
                String content = doc.getMetadata().getContentText();

                response
                    .append(i + 1)
                    .append(". ")
                    .append(title != null ? title : "文档")
                    .append("\n");
                // 限制内容长度
                if (content.length() > 500) {
                    content = content.substring(0, 500) + "...";
                }
                response.append(content).append("\n\n");
            }

            return response.toString();
        } catch (Exception e) {
            System.err.println("知识库检索失败: " + e.getMessage());
            e.printStackTrace();
            return "抱歉，检索知识库时发生错误，请稍后再试。";
        }
    }

    /**
     * 检查知识库是否已初始化
     *
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return knowledgeBase != null;
    }
}
