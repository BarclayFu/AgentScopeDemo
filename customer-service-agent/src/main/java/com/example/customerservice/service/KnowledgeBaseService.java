package com.example.customerservice.service;

import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 知识库服务
 * 提供基于RAG的文档检索和问答功能
 */
@Service
public class KnowledgeBaseService {

    private static final Logger logger = LoggerFactory.getLogger(
        KnowledgeBaseService.class
    );

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
            logger.error("知识库初始化失败", e);
        }
    }

    /**
     * 初始化知识库内容
     */
    private void initializeKnowledgeBase() {
        try {
            int addedCount = 0;
            int skippedCount = 0;

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

            // 售后服务政策（拆分为独立主题，避免一次命中返回整段）
            String returnPolicyContent = """
                退换货政策
                1. 自签收之日起7天内可无理由退货（特殊商品除外）
                2. 15天内出现质量问题可换货
                3. 退货商品需保持原包装完整，配件齐全
                4. 退货产生的运费由客户承担（质量问题除外）
                """;
            String warrantyPolicyContent = """
                保修政策
                1. iPhone整机保修1年
                2. MacBook整机保修2年
                3. AirPods整机保修1年
                4. 保修期内非人为损坏免费维修
                """;
            String repairPolicyContent = """
                维修服务
                1. 官方授权维修点提供维修服务
                2. 维修周期一般为1-2周
                3. 贵重物品建议提前备份数据
                4. 维修前可先通过智能客服查询常见问题
                """;

            // 将文档添加到知识库
            if (addDocumentToKnowledgeBase("常见问题与解答", faqContent)) {
                addedCount++;
            } else {
                skippedCount++;
            }
            if (addDocumentToKnowledgeBase("产品使用指南", productGuideContent)) {
                addedCount++;
            } else {
                skippedCount++;
            }
            if (addDocumentToKnowledgeBase("售后服务政策-退换货", returnPolicyContent)) {
                addedCount++;
            } else {
                skippedCount++;
            }
            if (addDocumentToKnowledgeBase("售后服务政策-保修", warrantyPolicyContent)) {
                addedCount++;
            } else {
                skippedCount++;
            }
            if (addDocumentToKnowledgeBase("售后服务政策-维修", repairPolicyContent)) {
                addedCount++;
            } else {
                skippedCount++;
            }

            logger.info(
                "知识库初始化完成，新增: {}，跳过(已存在): {}",
                addedCount,
                skippedCount
            );
        } catch (Exception e) {
            logger.error("知识库文档初始化失败", e);
        }
    }

    /**
     * 将文档添加到知识库
     *
     * @param title  文档标题
     * @param content 文档内容
     */
    private boolean addDocumentToKnowledgeBase(String title, String content) {
        try {
            if (isDocumentInitialized(title, content)) {
                logger.info("跳过知识文档初始化（已存在），title={}", title);
                return false;
            }

            TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
            List<Document> docs = reader.read(ReaderInput.fromString(content)).block();
            if (docs == null || docs.isEmpty()) {
                return false;
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
            logger.info("知识文档已写入，title={}, chunkCount={}", title, enrichedDocs.size());
            return true;
        } catch (Exception e) {
            logger.error("添加文档到知识库失败，title={}", title, e);
            return false;
        }
    }

    private boolean isDocumentInitialized(String title, String content) {
        try {
            String probe = firstNonBlankLine(content);
            if (probe == null) {
                return false;
            }

            RetrieveConfig config = RetrieveConfig.builder()
                .limit(5)
                .scoreThreshold(0.75)
                .build();

            List<Document> results = knowledgeBase.retrieve(probe, config).block();
            if (results == null || results.isEmpty()) {
                return false;
            }

            for (Document doc : results) {
                String existingTitle = doc.getPayloadValueAs("title", String.class);
                if (title.equals(existingTitle)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.warn("检查知识文档是否已初始化失败，title={}", title, e);
            return false;
        }
    }

    private String firstNonBlankLine(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String[] lines = text.split("\\R");
        for (String line : lines) {
            if (line != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return null;
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
                .limit(10) // 先多取一些，后续做去重
                .scoreThreshold(0.3) // 相似度阈值
                .build();

            // 执行检索
            List<Document> results = knowledgeBase
                .retrieve(question, config)
                .block();

            if (results == null || results.isEmpty()) {
                return "抱歉，知识库中没有找到与您的问题相关的信息。请尝试重新表述问题或联系人工客服。";
            }

            // 去重：同标题+同内容只保留一条，避免重复入库导致的多次展示
            List<Document> uniqueResults = deduplicateResults(results);
            if (uniqueResults.isEmpty()) {
                return "抱歉，知识库中没有找到与您的问题相关的信息。请尝试重新表述问题或联系人工客服。";
            }

            // 按提问焦点过滤，避免"问保修返回退换货"的泛化结果
            uniqueResults = filterResultsByQuestionFocus(question, uniqueResults);

            // 整合检索结果
            StringBuilder response = new StringBuilder();
            response.append("根据知识库中的信息，为您找到以下相关内容：\n\n");

            for (int i = 0; i < uniqueResults.size() && i < 3; i++) {
                Document doc = uniqueResults.get(i);
                String title = doc.getPayloadValueAs("title", String.class);
                String content = doc.getMetadata().getContentText();
                if (content == null) {
                    content = "";
                }

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
            logger.error("知识库检索失败，question={}", question, e);
            return "抱歉，检索知识库时发生错误，请稍后再试。";
        }
    }

    private List<Document> deduplicateResults(List<Document> results) {
        Set<String> seen = new LinkedHashSet<>();
        List<Document> unique = new ArrayList<>();
        for (Document doc : results) {
            String title = doc.getPayloadValueAs("title", String.class);
            String content = doc.getMetadata().getContentText();
            String key = (title != null ? title : "") + "\n" + (content != null ? content : "");
            if (seen.add(key)) {
                unique.add(doc);
            }
        }
        return unique;
    }

    private List<Document> filterResultsByQuestionFocus(
        String question,
        List<Document> results
    ) {
        if (question == null || question.isBlank() || results.isEmpty()) {
            return results;
        }

        String focus = null;
        if (question.contains("保修")) {
            focus = "保修";
        } else if (question.contains("退货") || question.contains("换货") || question.contains("退换")) {
            focus = "退换";
        } else if (question.contains("维修")) {
            focus = "维修";
        }

        if (focus == null) {
            return results;
        }

        List<Document> focused = new ArrayList<>();
        for (Document doc : results) {
            String title = doc.getPayloadValueAs("title", String.class);
            String content = doc.getMetadata().getContentText();
            String merged = (title != null ? title : "") + "\n" + (content != null ? content : "");
            if (merged.contains(focus)) {
                focused.add(doc);
            }
        }
        return focused.isEmpty() ? results : focused;
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
