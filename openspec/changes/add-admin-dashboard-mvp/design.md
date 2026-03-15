## Context
当前项目已经具备 Vue 聊天页和基础路由，但管理后台缺少真实数据支撑。后端监控服务仅暴露字符串统计，前端设置页也没有状态持久化，因此需要补上一层稳定的数据契约和页面行为约束。

## Goals
- 为仪表盘提供稳定、可扩展的 JSON 监控接口
- 让设置页修改能够跨刷新保留，并影响聊天页默认行为
- 保持改造范围收敛，不触碰 AgentScope 主流程

## Non-Goals
- 不引入外部观测平台 SDK
- 不在本轮实现知识库管理
- 不把前端设置同步到后端数据库

## Monitoring API Shape
建议将监控接口统一为 JSON 结构，至少包含以下字段：

```json
{
  "summary": {
    "activeSessions": 0,
    "totalMessages": 0,
    "totalToolCalls": 0,
    "errorCount": 0,
    "avgResponseTimeMs": 0,
    "lastMessageAt": null,
    "lastErrorAt": null
  },
  "checkedAt": 0
}
```

健康状态接口单独返回：

```json
{
  "service": "Customer Service Agent",
  "status": "UP",
  "checkedAt": 0
}
```

## Frontend Settings Strategy
- 使用独立的前端设置 store 管理后台偏好
- 设置项保存在 localStorage 中，保证刷新后保留
- API 地址解析顺序为：本地设置值 > `VITE_API_BASE_URL` > 空字符串
- 聊天页默认用户 ID、流式速度和 API 地址均从设置 store 读取

## Rollout Plan
1. 先完成后端监控接口 JSON 化
2. 再接通前端仪表盘页面
3. 最后实现设置持久化和测试
