package com.example.customerservice.service.extractor;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RulePreprocessor {

    private static final Pattern QA_PATTERN = Pattern.compile("\\?(.*)\\n[答案：:](.*)");
    private static final Pattern PRODUCT_PATTERN = Pattern.compile("[产品商品][：:]([^\n，,。]+)");
    private static final Pattern ORDER_PATTERN = Pattern.compile("[订单单号][：:]([A-Z0-9]+)");
    private static final Pattern SERVICE_PATTERN = Pattern.compile("(保修|质保|维修|退换|退款|退货|换货)[^。，,\n]*(?:的|是|方式|政策|条件|范围)?");

    public Map<String, List<String>> preprocess(String text) {
        Map<String, List<String>> extracted = new HashMap<>();
        extracted.put("products", extractProducts(text));
        extracted.put("orders", extractOrders(text));
        extracted.put("services", extractServices(text));
        extracted.put("qas", extractQAs(text));
        return extracted;
    }

    private List<String> extractProducts(String text) {
        List<String> products = new ArrayList<>();
        Matcher matcher = PRODUCT_PATTERN.matcher(text);
        while (matcher.find()) {
            products.add(matcher.group(1).trim());
        }
        return products;
    }

    private List<String> extractOrders(String text) {
        List<String> orders = new ArrayList<>();
        Matcher matcher = ORDER_PATTERN.matcher(text);
        while (matcher.find()) {
            orders.add(matcher.group(1).trim());
        }
        return orders;
    }

    private List<String> extractServices(String text) {
        List<String> services = new ArrayList<>();
        Matcher matcher = SERVICE_PATTERN.matcher(text);
        while (matcher.find()) {
            services.add(matcher.group(1).trim());
        }
        return services;
    }

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