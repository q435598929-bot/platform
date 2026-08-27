# Service Platform

本地服务平台大工程，统一收纳平台、AI 和 Task 的前后端源码、应用注册、数据库迁移与
Docker Compose 启停入口。原有 `ai-console`、`task-executor` 目录保持不变。

## 工程结构

```text
platform/
├─ apps/
│  ├─ platform/
│  │  ├─ platform-server/           # Java 21 / Spring Boot 平台目录与生命周期 API
│  │  └─ platform-web/              # React / TypeScript / Ant Design 平台页面
│  ├─ ai/
│  │  ├─ ai-server/                 # 从 ai-console 复制并适配
│  │  └─ ai-web/                    # 从 ai-console 复制并适配
│  └─ task/
│     ├─ task-server/               # 原 main() 任务 + Spring Boot 管理 API
│     └─ task-web/                  # Ant Design 任务管理页面
├─ registry/                        # 宿主机与容器应用注册表
├─ data/task/input|output/           # Compose 下 Task 输入、输出挂载目录
├─ compose.yaml
├─ pom.xml                          # 三个 Java Server 的 Maven Reactor
└─ package.json                     # 三个 Web 的 npm Workspaces
```

## 统一数据库

三个后端使用同一个 MySQL 数据库 `service_platform`，按应用前缀区分表：

| 前缀 | 归属 | 主要表 |
| --- | --- | --- |
| `platform_` | 平台 | 应用注册表、用户、角色、权限、令牌撤销记录 |
| `ai_` | AI | `ai_provider`、`ai_model`、`ai_request_log`、`ai_conversation*` |
| `task_` | Task | `task_definition`、`task_execution`、`task_execution_log` |

各模块分别使用 `platform_flyway_history`、`ai_flyway_history`、
`task_flyway_history`，互不干扰地管理迁移版本。

## Docker Compose 启动

Docker Desktop 安装并启动后，在工程根目录执行：

```powershell
cd D:\dev\code\paas\platform
Copy-Item .env.example .env
# 编辑 .env：替换数据库密码、管理员密码、PLATFORM_JWT_SECRET 和 AI_ENCRYPTION_KEY
docker compose up -d --build
docker compose ps
```

`PLATFORM_JWT_SECRET` 和 `AI_ENCRYPTION_KEY` 都应使用独立的随机 32 字节值并进行
Base64 编码，不要继续使用 `.env.example` 中的占位内容。

默认访问地址：

| 服务 | 地址 |
| --- | --- |
| Platform Web / API | `http://localhost:9091` / `http://localhost:9090` |
| AI Web / API | `http://localhost:9092` / `http://localhost:8081` |
| Task Web / API | `http://localhost:9093` / `http://localhost:8082` |
| MySQL | `localhost:3307`（容器内部仍为 `3306`） |

使用 `3307` 是为了避免与电脑上已有的 MySQL `3306` 冲突。停止服务使用
`docker compose stop`；停止并删除容器使用 `docker compose down`。数据库保存在命名卷
`platform-mysql`，普通 `down` 不会删除数据。

## 不使用 Docker 的本地开发

本机需要 Java 21、Maven、Node.js 和可访问的 MySQL。默认连接
`127.0.0.1:3306/service_platform`，开发默认账号为 `root/admin`；也可以通过
`PLATFORM_DATASOURCE_URL`、`PLATFORM_DATASOURCE_USERNAME`、
`PLATFORM_DATASOURCE_PASSWORD` 覆盖。

本地开发首次启动会创建 `admin / admin123456`。这只用于本机快速启动，登录后应在
“用户与权限”中立即重置密码；Compose 环境不提供这个默认密码，必须在 `.env` 中设置
`PLATFORM_ADMIN_PASSWORD`。

分别在三个终端启动后端：

```powershell
mvn -pl apps/platform/platform-server spring-boot:run
mvn -pl apps/ai/ai-server spring-boot:run
mvn -pl apps/task/task-server spring-boot:run
```

安装并启动前端：

```powershell
npm install
npm run dev:web     # http://localhost:5174
npm run dev:ai      # http://localhost:5175
npm run dev:task    # http://localhost:5176
```

## 登录、退出与权限

Platform Server 统一签发 JWT，三个 Web 共用 `PLATFORM_TOKEN` HttpOnly Cookie，也支持
`Authorization: Bearer <token>`。因此从平台登录后可以直接进入 AI 和 Task；退出、禁用用户、
修改用户角色或重置密码都会使已有令牌失效。

| 角色 | 默认能力 |
| --- | --- |
| `ADMIN` | 平台管理、生命周期、AI、Task 全部权限 |
| `OPERATOR` | 平台查看/启停、AI 配置与调用、Task 管理与执行 |
| `VIEWER` | 只读查看平台、AI 和 Task |

平台控台仅对 `ADMIN` 展示用户管理，可新增用户、分配角色、禁用/启用账号和重置密码。
后端也会逐接口校验权限，隐藏按钮不是唯一的安全措施。

## Task 双模式兼容

- 原有业务任务类及 `public static void main(String[] args)` 入口保留，可继续按原方式直接运行。
- `task-server` 会把明确白名单内的 19 个入口同步到 `task_definition`。
- 所有业务任务首次注册时均为禁用、高风险；通过页面执行前必须先启用并再次勾选确认。
- 同一时刻只允许一个兼容任务运行，避免原代码中的静态 SDK 配置互相污染。
- 页面/API 调用最终仍反射调用原 `main()`，不改动已有下游请求路径和请求契约。
- Task 页面会按任务显示结构化输入项，包括商户号列表、Excel/图片路径、输出目录、模式和
  原业务请求参数；必填项由后端校验，密码/令牌在执行历史中脱敏。
- 页面保留“高级 main 参数”，可以继续传入原来的命令行参数；直接运行 Java
  `main(String[] args)` 的方式同样保留。
- 文件型任务可使用 `TASK_INPUT_PATH`、`TASK_AUX_INPUT_PATH`、`TASK_OUTPUT_DIR`
  作为无页面输入时的后备配置；Compose 已挂载 `data/task/input` 与 `data/task/output`。

两个依赖 SDK Demo 内部示例类、当前无法由现有 SDK 编译的旧文件，只在新工程副本中放入
`task-server/legacy-sources/hezhao-sdk-demos` 留档，未加入可执行白名单；原
`task-executor` 中的文件没有修改。

## 构建与验证

```powershell
mvn test
npm run build:web
```

平台注册表位于 `registry/applications.yml`；容器环境使用
`registry/applications.compose.yml`。平台启动时会将注册表同步到 `platform_` 表。
# platform
