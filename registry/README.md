# Application registry

`applications.yml` 是宿主机本地开发使用的应用目录；`applications.compose.yml`
是容器内使用的等价目录。每个应用包含 `SERVER`、`WEB` 等组件，以及统一的启动、
停止、重启和访问链接。

当前注册 `platform`、`ai`、`task` 三个应用，源码均位于本仓库。注册表内容会在
平台启动或手动刷新时同步到 `platform_application`、`platform_component` 和
`platform_application_link` 表。
