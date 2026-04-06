package com.example.customerservice.service;

import com.example.customerservice.dto.KnowledgeEntryCreateRequest;
import com.example.customerservice.dto.KnowledgeEntryListResponse;
import com.example.customerservice.dto.KnowledgeEntryResponse;
import com.example.customerservice.dto.KnowledgeOperationResponse;
import com.example.customerservice.dto.KnowledgeStatusResponse;
import com.example.customerservice.dto.RetrievedChunk;
import com.example.customerservice.dto.VectorSearchResult;
import com.example.customerservice.service.KnowledgeGraphService;
import com.example.customerservice.service.extractor.TripleExtractor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.store.VDBStoreBase;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private static final String REGISTRY_SOURCE = "knowledge-console";
    private static final String ENTRY_TYPE = "text";
    private static final Path REGISTRY_PATH = Paths.get(
        "data",
        "knowledge-entries.json"
    );

    private final Knowledge knowledgeBase;
    private final ObjectMapper objectMapper;
    private final TripleExtractor tripleExtractor;
    private final KnowledgeGraphService knowledgeGraphService;
    private final Map<String, ManagedKnowledgeEntry> entries =
        new LinkedHashMap<>();

    private volatile Long lastUpdatedAt;
    private volatile Long lastRebuildAt;
    private volatile String lastOperationMessage = "知识库尚未执行管理操作";

    public KnowledgeBaseService(
        Knowledge knowledgeBase,
        ObjectMapper objectMapper,
        TripleExtractor tripleExtractor,
        KnowledgeGraphService knowledgeGraphService
    ) {
        this.knowledgeBase = knowledgeBase;
        this.objectMapper = objectMapper;
        this.tripleExtractor = tripleExtractor;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * 初始化知识库
     */
    @PostConstruct
    public synchronized void init() {
        try {
            loadRegistry();
            if (entries.isEmpty()) {
                seedDefaultEntries();
            }
            ensureManagedIndex();
            ensureManagedGraph();
            updateLastUpdatedAtFromEntries();
        } catch (Exception e) {
            logger.error("知识库初始化失败", e);
            lastOperationMessage = "知识库初始化失败: " + e.getMessage();
        }
    }

    public synchronized KnowledgeEntryListResponse listEntries() {
        List<KnowledgeEntryResponse> list = entries.values()
            .stream()
            .sorted(
                Comparator.comparingLong(ManagedKnowledgeEntry::updatedAt)
                    .reversed()
            )
            .map(entry ->
                new KnowledgeEntryResponse(
                    entry.entryId(),
                    entry.title(),
                    entry.content(),
                    preview(entry.content()),
                    entry.source(),
                    entry.type(),
                    List.of(),
                    List.of(),
                    entry.createdAt(),
                    entry.updatedAt()
                )
            )
            .toList();

        return new KnowledgeEntryListResponse(
            list,
            list.size(),
            Instant.now().toEpochMilli()
        );
    }

    public synchronized KnowledgeOperationResponse createEntry(
        KnowledgeEntryCreateRequest request
    ) throws IOException {
        String title = request != null ? request.getTitle() : null;
        String content = request != null ? request.getContent() : null;
        return createManagedEntry(title, content, REGISTRY_SOURCE);
    }

    public synchronized KnowledgeOperationResponse updateEntry(
        String entryId,
        KnowledgeEntryCreateRequest request
    ) throws IOException {
        ManagedKnowledgeEntry existing = entries.get(entryId);
        if (existing == null) {
            throw new IllegalArgumentException("知识条目不存在: " + entryId);
        }

        String title = request.getTitle();
        String content = request.getContent();

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("知识标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("知识内容不能为空");
        }

        long now = Instant.now().toEpochMilli();
        ManagedKnowledgeEntry updated = new ManagedKnowledgeEntry(
            existing.entryId(),
            title.trim(),
            content.trim(),
            existing.source(),
            existing.type(),
            existing.createdAt(),
            now,
            new ArrayList<>()
        );

        deleteIndexedChunks(existing.chunkIds());
        knowledgeGraphService.removeEntryReferences(entryId);

        entries.put(entryId, updated);

        try {
            indexEntry(updated);
            tripleExtractor.extractAndStore(
                updated.getEntryId(),
                updated.getTitle(),
                updated.getContent()
            );
            persistRegistry();
        } catch (Exception exception) {
            logger.error("更新知识条目失败，尝试回滚 entryId={}", entryId, exception);
            entries.put(entryId, existing);
            try {
                indexEntry(existing.withChunkIds(new ArrayList<>()));
                tripleExtractor.extractAndStore(
                    existing.getEntryId(),
                    existing.getTitle(),
                    existing.getContent()
                );
                persistRegistry();
            } catch (Exception rollbackException) {
                logger.error("知识条目更新回滚失败，entryId={}", entryId, rollbackException);
            }
            throw exception;
        }

        lastUpdatedAt = now;
        lastOperationMessage = "已更新知识条目: " + updated.title();

        return new KnowledgeOperationResponse(
            "知识条目已更新",
            entryId,
            now
        );
    }

    public synchronized KnowledgeOperationResponse createManagedEntry(
        String title,
        String content,
        String source
    ) throws IOException {
        validateEntry(title, content);

        long now = Instant.now().toEpochMilli();
        String entryId = "kb-" + UUID.randomUUID();
        ManagedKnowledgeEntry entry = new ManagedKnowledgeEntry(
            entryId,
            title.trim(),
            content.trim(),
            source,
            ENTRY_TYPE,
            now,
            now,
            new ArrayList<>()
        );

        entries.put(entryId, entry);
        try {
            indexEntry(entry);
            updateEntryChunkIds(entryId, entry.chunkIds());
            persistRegistry();
        } catch (Exception exception) {
            entries.remove(entryId);
            throw exception;
        }

        lastUpdatedAt = now;
        lastOperationMessage = "已新增知识条目: " + entry.title();

        // Extract triples to Neo4j
        try {
            tripleExtractor.extractAndStore(entry.getEntryId(), entry.getTitle(), entry.getContent());
        } catch (Exception e) {
            logger.warn("Failed to extract triples for entry {}: {}", entry.getEntryId(), e.getMessage());
        }

        return new KnowledgeOperationResponse(
            "知识条目已创建",
            entryId,
            now
        );
    }

    public synchronized KnowledgeOperationResponse deleteEntry(String entryId) throws IOException {
        ManagedKnowledgeEntry entry = entries.remove(entryId);
        if (entry == null) {
            throw new IllegalArgumentException("知识条目不存在: " + entryId);
        }

        deleteIndexedChunks(entry.chunkIds());
        knowledgeGraphService.removeEntryReferences(entryId);
        persistRegistry();

        long now = Instant.now().toEpochMilli();
        lastUpdatedAt = now;
        lastOperationMessage = "已删除知识条目: " + entry.title();
        return new KnowledgeOperationResponse(
            "知识条目已删除",
            entryId,
            now
        );
    }

    public synchronized KnowledgeOperationResponse rebuildKnowledgeBase() throws IOException {
        entries.values().forEach(entry -> {
            deleteIndexedChunks(entry.chunkIds());
            knowledgeGraphService.removeEntryReferences(entry.entryId());
        });
        entries.replaceAll((entryId, entry) -> entry.withChunkIds(new ArrayList<>()));

        for (ManagedKnowledgeEntry entry : entries.values()) {
            indexEntry(entry);
            try {
                tripleExtractor.extractAndStore(
                    entry.getEntryId(),
                    entry.getTitle(),
                    entry.getContent()
                );
            } catch (Exception e) {
                logger.warn(
                    "知识条目重建图谱失败，entryId={}, title={}",
                    entry.getEntryId(),
                    entry.getTitle(),
                    e
                );
            }
        }

        persistRegistry();

        long now = Instant.now().toEpochMilli();
        lastRebuildAt = now;
        lastUpdatedAt = now;
        lastOperationMessage =
            "知识库已刷新，共重建 " + entries.size() + " 条知识条目";
        return new KnowledgeOperationResponse(
            "知识库刷新完成",
            null,
            now
        );
    }

    public synchronized KnowledgeStatusResponse getStatus() {
        return new KnowledgeStatusResponse(
            isInitialized(),
            entries.size(),
            lastUpdatedAt,
            lastRebuildAt,
            lastOperationMessage,
            Instant.now().toEpochMilli()
        );
    }

    /**
     * 根据问题检索相关文档
     *
     * @param question 用户问题
     * @return 相关文档内容
     */
    public synchronized String searchKnowledgeBase(String question) {
        try {
            RetrieveConfig config = RetrieveConfig.builder()
                .limit(10)
                .scoreThreshold(0.3)
                .build();

            List<Document> results = knowledgeBase
                .retrieve(question, config)
                .block();

            if (results == null || results.isEmpty()) {
                return "抱歉，知识库中没有找到与您的问题相关的信息。请尝试重新表述问题或联系人工客服。";
            }

            List<Document> managedResults = filterManagedResults(results);
            List<Document> uniqueResults = deduplicateResults(
                managedResults.isEmpty() ? results : managedResults
            );

            if (uniqueResults.isEmpty()) {
                return "抱歉，知识库中没有找到与您的问题相关的信息。请尝试重新表述问题或联系人工客服。";
            }

            uniqueResults = filterResultsByQuestionFocus(question, uniqueResults);

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

    /**
     * 检索知识库并返回结构化结果
     *
     * @param question 用户问题
     * @param limit 返回结果数量限制
     * @return 结构化检索结果
     */
    public synchronized VectorSearchResult searchKnowledgeBaseStructured(String question, int limit) {
        try {
            RetrieveConfig config = RetrieveConfig.builder()
                .limit(limit > 0 ? limit : 10)
                .scoreThreshold(0.3)
                .build();

            List<Document> results = knowledgeBase.retrieve(question, config).block();
            List<RetrievedChunk> chunks = new ArrayList<>();

            if (results != null) {
                for (Document doc : results) {
                    String content = doc.getMetadata().getContentText();
                    double score = doc.getScore();
                    String title = doc.getPayloadValueAs("title", String.class);
                    chunks.add(new RetrievedChunk(content != null ? content : "", score, title));
                }
            }

            String answer = chunks.isEmpty()
                ? "抱歉，知识库中没有找到与您的问题相关的信息。"
                : "根据知识库中的信息，为您找到以下相关内容：\n\n" +
                  String.join("\n\n", chunks.stream().limit(3).map(RetrievedChunk::getContent).toList());

            return new VectorSearchResult(answer, chunks);
        } catch (Exception e) {
            logger.error("知识库检索失败，question={}", question, e);
            return new VectorSearchResult("抱歉，检索知识库时发生错误，请稍后再试。", List.of());
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

    /**
     * 获取知识条目的标题
     *
     * @param entryId 知识条目ID
     * @return 标题，如果不存在则返回null
     */
    public String getEntryTitle(String entryId) {
        ManagedKnowledgeEntry entry = entries.get(entryId);
        return entry != null ? entry.title() : null;
    }

    private void seedDefaultEntries() throws IOException {
        logger.info("知识库注册表为空，开始写入默认知识条目");
        long now = Instant.now().toEpochMilli();
        for (SeedEntry seed : defaultSeedEntries()) {
            String entryId = "kb-default-" + UUID.randomUUID();
            entries.put(
                entryId,
                new ManagedKnowledgeEntry(
                    entryId,
                    seed.title(),
                    seed.content(),
                    "seed",
                    ENTRY_TYPE,
                    now,
                    now,
                    new ArrayList<>()
                )
            );
        }
        persistRegistry();
        lastUpdatedAt = now;
        lastOperationMessage = "默认知识条目初始化完成";
    }

    private void ensureManagedIndex() throws IOException {
        for (ManagedKnowledgeEntry entry : entries.values()) {
            if (entry.chunkIds().isEmpty()) {
                indexEntry(entry);
            }
        }
        persistRegistry();
    }

    private void ensureManagedGraph() {
        for (ManagedKnowledgeEntry entry : entries.values()) {
            try {
                if (
                    knowledgeGraphService.findEntityIdsByEntryId(entry.entryId())
                        .isEmpty()
                ) {
                    tripleExtractor.extractAndStore(
                        entry.getEntryId(),
                        entry.getTitle(),
                        entry.getContent()
                    );
                }
            } catch (Exception e) {
                logger.warn(
                    "初始化知识图谱失败，entryId={}, title={}",
                    entry.getEntryId(),
                    entry.getTitle(),
                    e
                );
            }
        }
    }

    private void indexEntry(ManagedKnowledgeEntry entry) {
        try {
            TextReader reader = new TextReader(
                512,
                SplitStrategy.PARAGRAPH,
                50
            );
            List<Document> docs = reader
                .read(ReaderInput.fromString(entry.content()))
                .block();
            if (docs == null || docs.isEmpty()) {
                throw new IllegalStateException("未能为知识条目生成有效文档");
            }

            List<String> chunkIds = new ArrayList<>();
            List<Document> managedDocs = new ArrayList<>();
            for (int i = 0; i < docs.size(); i++) {
                Document doc = docs.get(i);
                String chunkId = entry.entryId() + "-chunk-" + i;
                chunkIds.add(chunkId);

                DocumentMetadata metadata = DocumentMetadata.builder()
                    .content(doc.getMetadata().getContent())
                    .docId(entry.entryId())
                    .chunkId(chunkId)
                    .payload(
                        Map.of(
                            "source",
                            entry.source(),
                            "type",
                            entry.type(),
                            "title",
                            entry.title(),
                            "entryId",
                            entry.entryId()
                        )
                    )
                    .build();
                managedDocs.add(new Document(metadata));
            }

            knowledgeBase.addDocuments(managedDocs).block();
            updateEntryChunkIds(entry.entryId(), chunkIds);
            logger.info(
                "知识条目已索引，entryId={}, title={}, chunkCount={}",
                entry.entryId(),
                entry.title(),
                chunkIds.size()
            );
        } catch (Exception e) {
            logger.error("知识条目索引失败，entryId={}", entry.entryId(), e);
            throw new IllegalStateException("知识条目索引失败: " + e.getMessage(), e);
        }
    }

    private void updateEntryChunkIds(String entryId, List<String> chunkIds) {
        ManagedKnowledgeEntry existing = entries.get(entryId);
        if (existing == null) {
            return;
        }
        entries.put(
            entryId,
            existing.withChunkIds(chunkIds).touch(Instant.now().toEpochMilli())
        );
    }

    private void deleteIndexedChunks(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }

        VDBStoreBase store = extractStore();
        if (store == null) {
            logger.warn("当前 Knowledge 实现不支持直接删除向量条目");
            return;
        }

        for (String chunkId : chunkIds) {
            try {
                store.delete(chunkId).block();
            } catch (Exception e) {
                logger.warn("删除知识条目 chunk 失败，chunkId={}", chunkId, e);
            }
        }
    }

    private VDBStoreBase extractStore() {
        if (knowledgeBase instanceof SimpleKnowledge simpleKnowledge) {
            return simpleKnowledge.getEmbeddingStore();
        }
        return null;
    }

    private List<Document> filterManagedResults(List<Document> results) {
        Set<String> activeEntryIds = entries.keySet();
        return results.stream()
            .filter(doc -> {
                String entryId = doc.getPayloadValueAs("entryId", String.class);
                return entryId != null && activeEntryIds.contains(entryId);
            })
            .collect(Collectors.toList());
    }

    private List<Document> deduplicateResults(List<Document> results) {
        Set<String> seen = new LinkedHashSet<>();
        List<Document> unique = new ArrayList<>();
        for (Document doc : results) {
            String title = doc.getPayloadValueAs("title", String.class);
            String content = doc.getMetadata().getContentText();
            String key =
                (title != null ? title : "") +
                "\n" +
                (content != null ? content : "");
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
        } else if (
            question.contains("退货") ||
            question.contains("换货") ||
            question.contains("退换")
        ) {
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
            String merged =
                (title != null ? title : "") +
                "\n" +
                (content != null ? content : "");
            if (merged.contains(focus)) {
                focused.add(doc);
            }
        }
        return focused.isEmpty() ? results : focused;
    }

    private void loadRegistry() throws IOException {
        if (!Files.exists(REGISTRY_PATH)) {
            Files.createDirectories(REGISTRY_PATH.getParent());
            return;
        }

        List<ManagedKnowledgeEntry> storedEntries = objectMapper.readValue(
            REGISTRY_PATH.toFile(),
            new TypeReference<List<ManagedKnowledgeEntry>>() {}
        );
        entries.clear();
        for (ManagedKnowledgeEntry entry : storedEntries) {
            entries.put(entry.entryId(), entry);
        }
    }

    private void persistRegistry() throws IOException {
        Files.createDirectories(REGISTRY_PATH.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(
            REGISTRY_PATH.toFile(),
            entries.values()
        );
    }

    private void updateLastUpdatedAtFromEntries() {
        lastUpdatedAt = entries.values()
            .stream()
            .map(ManagedKnowledgeEntry::updatedAt)
            .max(Long::compareTo)
            .orElse(null);
    }

    private String preview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 120
            ? normalized.substring(0, 120) + "..."
            : normalized;
    }

    private void validateEntry(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("知识标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("知识内容不能为空");
        }
    }

    private List<SeedEntry> defaultSeedEntries() {
        return List.of(
            new SeedEntry(
                "常见问题与解答",
                """
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
                """
            ),
            new SeedEntry(
                "产品使用指南",
                """
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
                """
            ),
            new SeedEntry(
                "售后服务政策-退换货",
                """
                退换货政策
                1. 自签收之日起7天内可无理由退货（特殊商品除外）
                2. 15天内出现质量问题可换货
                3. 退货商品需保持原包装完整，配件齐全
                4. 退货产生的运费由客户承担（质量问题除外）
                """
            ),
            new SeedEntry(
                "售后服务政策-保修",
                """
                保修政策
                1. iPhone整机保修1年
                2. MacBook整机保修2年
                3. AirPods整机保修1年
                4. 保修期内非人为损坏免费维修
                """
            ),
            new SeedEntry(
                "售后服务政策-维修",
                """
                维修服务
                1. 官方授权维修点提供维修服务
                2. 维修周期一般为1-2周
                3. 贵重物品建议提前备份数据
                4. 维修前可先通过智能客服查询常见问题
                """
            )
        );
    }

    private static class SeedEntry {

        private final String title;
        private final String content;

        private SeedEntry(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String title() {
            return title;
        }

        public String content() {
            return content;
        }
    }

    public static class ManagedKnowledgeEntry {

        private String entryId;
        private String title;
        private String content;
        private String source;
        private String type;
        private long createdAt;
        private long updatedAt;
        private List<String> chunkIds;

        public ManagedKnowledgeEntry() {}

        public ManagedKnowledgeEntry(
            String entryId,
            String title,
            String content,
            String source,
            String type,
            long createdAt,
            long updatedAt,
            List<String> chunkIds
        ) {
            this.entryId = entryId;
            this.title = title;
            this.content = content;
            this.source = source;
            this.type = type;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.chunkIds = chunkIds;
        }

        public String entryId() {
            return entryId;
        }

        public String title() {
            return title;
        }

        public String content() {
            return content;
        }

        public String source() {
            return source;
        }

        public String type() {
            return type;
        }

        public long createdAt() {
            return createdAt;
        }

        public long updatedAt() {
            return updatedAt;
        }

        public List<String> chunkIds() {
            return chunkIds == null ? new ArrayList<>() : chunkIds;
        }

        public ManagedKnowledgeEntry withChunkIds(List<String> newChunkIds) {
            return new ManagedKnowledgeEntry(
                entryId,
                title,
                content,
                source,
                type,
                createdAt,
                updatedAt,
                new ArrayList<>(newChunkIds)
            );
        }

        public ManagedKnowledgeEntry touch(long timestamp) {
            return new ManagedKnowledgeEntry(
                entryId,
                title,
                content,
                source,
                type,
                createdAt,
                timestamp,
                chunkIds()
            );
        }

        public String getEntryId() {
            return entryId;
        }

        public void setEntryId(String entryId) {
            this.entryId = entryId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
        }

        public List<String> getChunkIds() {
            return chunkIds;
        }

        public void setChunkIds(List<String> chunkIds) {
            this.chunkIds = chunkIds;
        }
    }
}
