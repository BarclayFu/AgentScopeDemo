package com.example.customerservice.service.extractor;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则预处理器
 *
 * 使用正则表达式从文本中快速提取结构化的知识信息。
 * 这是知识抽取管道的第一环，用于快速识别文本中的：
 * - 产品名称
 * - 订单编号
 * - 服务类型（保修、质保、维修等）
 * - 问答对
 *
 * 与LLM抽取相比，规则抽取速度快但覆盖范围有限，
 * 适合处理格式规范的文本。
 */
@Component
public class RulePreprocessor {

    /**
     * 问答对提取正则
     *
     * 匹配格式：问号结尾的问题 + 换行 + "答案："或"答："开头的内容
     *
     * 例如：
     * "智能手表如何保修？\n答案：联系客服处理"
     * 匹配结果：问题="智能手表如何保修"，答案="联系客服处理"
     */
    private static final Pattern QA_PATTERN = Pattern.compile("\\?(.*)\\n[答案：:](.*)");

    /**
     * 产品名称提取正则
     *
     * 匹配"[产品/商品]：[产品名]"格式
     *
     * 例如："产品：智能手表" -> 提取出"智能手表"
     */
    private static final Pattern PRODUCT_PATTERN = Pattern.compile("[产品商品][：:]([^\n，,。]+)");

    /**
     * 订单编号提取正则
     *
     * 匹配"[订单/单号]：[订单号]"格式，订单号通常为大写字母和数字的组合
     *
     * 例如："订单：ORD20240315001" -> 提取出"ORD20240315001"
     */
    private static final Pattern ORDER_PATTERN = Pattern.compile("[订单单号][：:]([A-Z0-9]+)");

    /**
     * 服务类型提取正则
     *
     * 匹配服务相关词汇：保修、质保、维修、退换、退款、退货、换货
     * 可选地匹配后面跟着的修饰词：的、是、政策、条件等
     *
     * 例如：
     * "保修两年" -> 提取出"保修"
     * "退换货政策" -> 提取出"退换"
     */
    private static final Pattern SERVICE_PATTERN = Pattern.compile("(保修|质保|维修|退换|退款|退货|换货)[^。，,\n]*(?:的|是|方式|政策|条件|范围)?");

    /**
     * 从文本中提取结构化信息
     *
     * @param text 输入文本
     * @return 包含以下键的Map：
     *         - products: List<String> 产品名称列表
     *         - orders: List<String> 订单编号列表
     *         - services: List<String> 服务类型列表
     *         - qas: List<Map<String, String>> 问答对列表，每项包含question和answer
     */
    public Map<String, Object> preprocess(String text) {
        Map<String, Object> extracted = new HashMap<>();
        extracted.put("products", extractProducts(text));
        extracted.put("orders", extractOrders(text));
        extracted.put("services", extractServices(text));
        extracted.put("qas", extractQAs(text));
        return extracted;
    }

    /**
     * 提取产品名称
     *
     * @param text 输入文本
     * @return 产品名称列表
     */
    private List<String> extractProducts(String text) {
        List<String> products = new ArrayList<>();
        Matcher matcher = PRODUCT_PATTERN.matcher(text);
        while (matcher.find()) {
            products.add(matcher.group(1).trim());
        }
        return products;
    }

    /**
     * 提取订单编号
     *
     * @param text 输入文本
     * @return 订单编号列表
     */
    private List<String> extractOrders(String text) {
        List<String> orders = new ArrayList<>();
        Matcher matcher = ORDER_PATTERN.matcher(text);
        while (matcher.find()) {
            orders.add(matcher.group(1).trim());
        }
        return orders;
    }

    /**
     * 提取服务类型
     *
     * @param text 输入文本
     * @return 服务类型列表
     */
    private List<String> extractServices(String text) {
        List<String> services = new ArrayList<>();
        Matcher matcher = SERVICE_PATTERN.matcher(text);
        while (matcher.find()) {
            services.add(matcher.group(1).trim());
        }
        return services;
    }

    /**
     * 提取问答对
     *
     * @param text 输入文本
     * @return 问答对列表，每个Map包含"question"和"answer"两个键
     */
    private List<Map<String, String>> extractQAs(String text) {
        List<Map<String, String>> qas = new ArrayList<>();
        Matcher matcher = QA_PATTERN.matcher(text);
        while (matcher.find()) {
            Map<String, String> qa = new HashMap<>();
            qa.put("question", matcher.group(1).trim());
            qa.put("answer", matcher.group(2).trim());
            qas.add(qa);
        }
        return qas;
    }
}