# ai-server

由原 `ai-console-server` 复制并独立改造，原项目保持不变。API 合同及 AI
下游调用行为保持兼容，数据改存 `service_platform` 库的 `ai_` 前缀表。

统一使用 Java 21、Spring Boot、JPA 和 Flyway，默认端口 `8081`。
