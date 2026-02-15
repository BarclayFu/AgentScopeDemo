package com.example.customerservice.controller;

import com.example.customerservice.service.ChatSessionService;
import io.agentscope.core.message.Msg;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 聊天控制器
 * 提供REST API接口处理用户消息
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // 允许跨域请求
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(
        ChatController.class
    );

    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * 处理用户发送的消息
     *
     * @param request 包含userId和message的请求体
     * @return Agent回复的内容
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> handleMessage(
        @RequestBody Map<String, String> request
    ) {
        String userId = request.get("userId");
        try {

            String message = request.get("message");

            logger.info("收到用户 {} 的消息: {}", userId, message);

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "用户ID不能为空")
                );
            }

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "消息内容不能为空")
                );
            }

            // 处理用户消息
            Msg response = chatSessionService.processUserMessage(
                userId,
                message
            );

            logger.info(
                "向用户 {} 发送响应，响应长度: {}",
                userId,
                response.getTextContent().length()
            );

            // 构造响应
            Map<String, Object> responseBody = Map.of(
                "userId",
                userId,
                "response",
                response.getTextContent(),
                "timestamp",
                System.currentTimeMillis()
            );

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            // 记录错误日志
            logger.error(
                "处理用户 {} 的消息时发生错误: {}",
                userId,
                e.getMessage(),
                e
            );

            return ResponseEntity.status(500).body(
                Map.of("error", "处理消息时发生错误: " + e.getMessage())
            );
        }
    }

    /**
     * 获取当前活跃会话数量
     *
     * @return 活跃会话数量
     */
    @GetMapping("/sessions/count")
    public ResponseEntity<Map<String, Object>> getActiveSessionCount() {
        logger.info("查询活跃会话数量");
        int count = chatSessionService.getActiveSessionCount();
        logger.info("当前活跃会话数量: {}", count);
        return ResponseEntity.ok(Map.of("activeSessions", count));
    }

    /**
     * 清除指定用户的会话（用于测试）
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/session/{userId}")
    public ResponseEntity<Map<String, Object>> clearUserSession(
        @PathVariable String userId
    ) {
        logger.info("清除用户 {} 的会话", userId);
        chatSessionService.clearUserSession(userId);
        logger.info("用户 {} 的会话已清除", userId);
        return ResponseEntity.ok(Map.of("message", "用户会话已清除"));
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("健康检查请求");
        return ResponseEntity.ok(
            Map.of("status", "healthy", "service", "Customer Service Agent")
        );
    }
}
