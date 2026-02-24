package com.example.customerservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 聊天请求DTO
 * 用于接收客户端的聊天消息请求
 */
public class ChatRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 是否启用流式响应（默认为true）
     */
    private boolean stream = true;

    /**
     * 模拟流式输出的字符间隔（毫秒）
     * 用于在Agent不支持真实流式时模拟打字机效果
     */
    private int streamInterval = 30;

    public ChatRequest() {
    }

    public ChatRequest(String userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public ChatRequest(String userId, String message, boolean stream) {
        this.userId = userId;
        this.message = message;
        this.stream = stream;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public int getStreamInterval() {
        return streamInterval;
    }

    public void setStreamInterval(int streamInterval) {
        this.streamInterval = streamInterval;
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "userId='" + userId + '\'' +
                ", message='" + message + '\'' +
                ", stream=" + stream +
                ", streamInterval=" + streamInterval +
                '}';
    }
}
