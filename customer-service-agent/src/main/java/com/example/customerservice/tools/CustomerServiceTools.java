package com.example.customerservice.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 客服工具类，包含处理常见客服场景的工具方法
 * 这些工具将被Agent调用以处理客户请求
 */
public class CustomerServiceTools {

    // 模拟订单数据库
    private static final Map<String, Order> orderDatabase = new HashMap<>();

    // 初始化一些示例订单数据
    static {
        orderDatabase.put("ORD001", new Order("ORD001", "iPhone 15 Pro", 999.99, "已发货", "2024-01-15"));
        orderDatabase.put("ORD002", new Order("ORD002", "MacBook Air M2", 1199.99, "处理中", "2024-01-10"));
        orderDatabase.put("ORD003", new Order("ORD003", "AirPods Pro", 249.99, "已完成", "2024-01-05"));
    }

    /**
     * 查询订单状态工具
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    @Tool(name = "query_order_status", description = "查询订单状态和详情")
    public static String queryOrderStatus(
            @ToolParam(name = "orderId", description = "订单ID，格式如ORD001") String orderId) {

        Order order = orderDatabase.get(orderId);
        if (order != null) {
            return String.format("订单ID: %s\n商品: %s\n价格: $%.2f\n状态: %s\n下单日期: %s",
                               order.id(), order.productName(), order.price(),
                               order.status(), order.orderDate());
        } else {
            return String.format("未找到订单ID为 %s 的订单，请检查订单号是否正确。", orderId);
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
    public static String processRefund(
            @ToolParam(name = "orderId", description = "订单ID") String orderId,
            @ToolParam(name = "reason", description = "退款原因") String reason) {

        Order order = orderDatabase.get(orderId);
        if (order == null) {
            return String.format("未找到订单ID为 %s 的订单，无法处理退款。", orderId);
        }

        // 生成退款编号
        String refundId = "REF" + System.currentTimeMillis() % 1000000;

        // 模拟退款处理逻辑
        return String.format("退款请求已受理\n退款编号: %s\n订单ID: %s\n商品: %s\n退款原因: %s\n" +
                           "预计1-3个工作日内处理完成，请注意查收退款款项。",
                           refundId, order.id(), order.productName(), reason);
    }

    /**
     * 查询产品信息工具
     *
     * @param productName 产品名称
     * @return 产品信息
     */
    @Tool(name = "query_product_info", description = "查询产品详细信息")
    public static String queryProductInfo(
            @ToolParam(name = "productName", description = "产品名称") String productName) {

        // 模拟产品数据库查询
        Map<String, String> productInfo = new HashMap<>();
        productInfo.put("iPhone 15 Pro", "iPhone 15 Pro搭载A17 Pro芯片，配备超瓷晶面板，支持5G网络，后置三摄系统。");
        productInfo.put("MacBook Air M2", "MacBook Air M2采用苹果M2芯片，13.6英寸 Liquid Retina 显示屏，轻薄便携。");
        productInfo.put("AirPods Pro", "AirPods Pro主动降噪耳机，支持空间音频，自适应通透模式。");

        String info = productInfo.get(productName);
        if (info != null) {
            return String.format("产品名称: %s\n产品信息: %s", productName, info);
        } else {
            return String.format("抱歉，未找到产品 %s 的详细信息。", productName);
        }
    }

    /**
     * 查询物流信息工具
     *
     * @param orderId 订单ID
     * @return 物流信息
     */
    @Tool(name = "query_shipping_status", description = "查询订单物流状态")
    public static String queryShippingStatus(
            @ToolParam(name = "orderId", description = "订单ID") String orderId) {

        Order order = orderDatabase.get(orderId);
        if (order == null) {
            return String.format("未找到订单ID为 %s 的订单，无法查询物流信息。", orderId);
        }

        // 模拟物流信息
        String[] shippingStatuses = {
            "已发货，预计1-2天送达",
            "运输中，已到达配送中心",
            "正在派送中，请保持电话畅通",
            "已送达"
        };

        Random random = new Random();
        String status = shippingStatuses[random.nextInt(shippingStatuses.length)];
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return String.format("订单ID: %s\n物流状态: %s\n更新时间: %s", orderId, status, currentTime);
    }

    /**
     * 订单记录类
     */
    public record Order(String id, String productName, double price, String status, String orderDate) {}
}
