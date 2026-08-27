# Managed applications

业务应用源码已经纳入平台仓库：

```text
apps/
  platform/
    platform-server/
    platform-web/
  ai/
    ai-server/
    ai-web/
  task/
    task-server/
    task-web/
```

`ai-server`、`ai-web` 从原 `ai-console` 复制并适配；`task-server` 保留原有
`public static void main(String[] args)` 入口，同时增加统一的 Spring Boot 管理 API；
`task-web` 提供任务启停、确认执行、历史与日志页面。

原 `ai-console` 与 `task-executor` 目录不移动、不删除，便于对照学习和独立运行。
