package com.example.customerservice.controller;

import com.example.customerservice.service.ChatSessionService;
import io.agentscope.core.message.Msg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 调试控制器
 * 提供调试和测试Agent行为的API接口
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * 直接测试工具调用
     */
    @PostMapping("/test-tool")
    public ResponseEntity<String> testToolDirectly(
            @RequestParam String userId,
            @RequestParam String message) {
        try {
            Msg response = chatSessionService.processUserMessage(userId, message);
            return ResponseEntity.ok(response.getTextContent());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("测试失败: " + e.getMessage());
        }
    }

    /**
     * 测试特定工具
     */
    @PostMapping("/test-shipping")
    public ResponseEntity<String> testShippingQuery(
            @RequestParam String userId,
            @RequestParam String orderId) {
        String message = "查询订单" + orderId + "的物流状态";
        try {
            Msg response = chatSessionService.processUserMessage(userId, message);
            return ResponseEntity.ok(response.getTextContent());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("测试失败: " + e.getMessage());
        }
    }

    /**
     * 清除会话并重新测试
     */
    @PostMapping("/reset-and-test")
    public ResponseEntity<String> resetAndTest(
            @RequestParam String userId,
            @RequestParam String message) {
        try {
            // 清除现有会话
            chatSessionService.clearUserSession(userId);

            // 处理新消息
            Msg response = chatSessionService.processUserMessage(userId, message);
            return ResponseEntity.ok(response.getTextContent());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("测试失败: " + e.getMessage());
        }
    }
}
