## Context
现有知识库能力主要聚焦在问答检索上，缺乏管理入口。`KnowledgeBaseService` 负责初始化示例文档和执行检索，但并未暴露管理能力，前端 `KnowledgeView` 也仅是占位页。为了让知识库从“内置能力”升级为“可运营能力”，需要增加一组轻量但完整的管理接口和对应页面。

## Goals
- 提供最小可用的知识库管理台
- 允许在前端查看知识条目、添加文本知识、删除知识和刷新索引
- 保持现有检索和聊天链路稳定

## Non-Goals
- 不实现复杂文档工作流和审批流
- 不实现多租户权限控制
- 不支持大规模批量导入或对象存储编排

## Proposed API Shape
建议至少提供以下管理接口：

- `GET /api/knowledge/entries`
  - 返回知识条目列表、总数、最近更新时间
- `POST /api/knowledge/entries`
  - 新增一条文本型知识，字段至少包含标题和内容
- `DELETE /api/knowledge/entries/{entryId}`
  - 删除指定知识条目
- `POST /api/knowledge/rebuild`
  - 触发知识库刷新或重建
- `GET /api/knowledge/status`
  - 返回知识库初始化状态、条目数、最近更新时间和最近一次操作结果

## Frontend Scope
知识库管理台 MVP 页面包含以下区域：
- 顶部状态区：显示知识库状态、总条目数、最近更新时间
- 条目列表区：展示标题、来源、更新时间、操作按钮
- 新增区：最小表单，支持标题和内容提交
- 操作区：支持刷新索引和删除条目

## Rollout Plan
1. 先定义后端管理接口和状态结构
2. 再接通知识库管理台页面
3. 最后补测试与文档
