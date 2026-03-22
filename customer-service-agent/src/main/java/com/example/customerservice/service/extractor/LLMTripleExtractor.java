package com.example.customerservice.service.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LLMTripleExtractor {
    private static final Logger logger = LoggerFactory.getLogger(LLMTripleExtractor.class);
    private static final String SYSTEM_PROMPT = """
            你是一个知识图谱抽取专家。你的任务是从给定文本中提取实体和关系，输出JSON格式的三元组。

            实体类型: Product, Service, QA, Concept
            关系类型: BELONGS_TO, HAS_SERVICE, RELATED_TO, REFERENCES, MENTIONS

            要求:
            1. 实体名称应简洁明了
            2. 关系必须来自上述关系类型列表
            3. 每个三元组表示 (实体A, 关系, 实体B)
            4. 输出纯JSON数组，不要包含其他文字
            """;

    private final OpenAIChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LLMTripleExtractor(OpenAIChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public List<Map<String, String>> extractTriples(String text) {
        String userPrompt = "输入文本: " + text + "\n\n请提取三元组，输出JSON数组:";

        List<Msg> messages = List.of(
            Msg.of(MsgRole.System, TextBlock.of(SYSTEM_PROMPT)),
            Msg.of(MsgRole.User, TextBlock.of(userPrompt))
        );

        String response = chatModel.chat(messages).get(0).getTextContent();

        return parseTriples(response);
    }

    private List<Map<String, String>> parseTriples(String response) {
        List<Map<String, String>> triples = new ArrayList<>();
        try {
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');
            if (start != -1 && end != -1) {
                String jsonArray = response.substring(start, end + 1);
                JsonNode node = objectMapper.readTree(jsonArray);
                if (node.isArray()) {
                    for (JsonNode item : node) {
                        Map<String, String> triple = new HashMap<>();
                        triple.put("subject", item.has("subject") ? item.get("subject").asText() : item.get("s").asText());
                        triple.put("relation", item.has("relation") ? item.get("relation").asText() : item.get("p").asText());
                        triple.put("object", item.has("object") ? item.get("object").asText() : item.get("o").asText());
                        triples.add(triple);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse LLM response as JSON: {}", response, e);
        }
        return triples;
    }
}