# 云平台与 Service Platform 技术详解

## 1. 文档目标

本文用于系统理解云平台、容器化、Docker Compose、Kubernetes、微服务、网络、
存储、数据库、DevOps、可观测性与安全等相关技术，并结合当前 `platform`
工程说明这些概念如何落地。

本文将内容分为三层：

1. 当前工程已经使用的技术与运行方式；
2. 从单机 Docker Compose 向生产级平台演进时需要补充的能力；
3. 各类技术的职责边界、常见选型和学习路径。

## 2. 什么是云平台

云平台不是单一软件，而是一套把计算、网络、存储、应用和运维能力统一管理起来的系统。
它通常提供资源申请、应用部署、服务治理、监控告警、权限管理和自动化交付等能力。

按管理对象可以分为三类：

| 类型 | 管理对象 | 使用者关心的内容 | 典型技术或产品 |
| --- | --- | --- | --- |
| IaaS | 虚拟机、网络、磁盘、物理机 | CPU、内存、IP、云盘 | OpenStack、VMware、AWS EC2 |
| PaaS | 应用、容器、中间件、发布流程 | 代码、配置、实例数、版本 | Kubernetes、OpenShift、Cloud Foundry |
| SaaS | 可直接使用的软件功能 | 账号、业务数据、权限 | 邮箱、CRM、在线文档 |

当前 `platform` 更接近 PaaS 管理控制台的早期形态：它统一收纳多个应用，提供
注册信息、生命周期入口、用户权限以及 AI、Task 等业务能力，但目前主要运行在单台电脑的
Docker Compose 环境中，还不是完整的多节点云平台。

## 3. 虚拟化、容器化与编排的区别

### 3.1 虚拟机

虚拟机通过 Hyper-V、KVM、VMware 等虚拟化程序模拟一台完整计算机。每台虚拟机拥有
独立操作系统和内核，隔离程度高，但启动较慢、占用资源较多。

```text
物理服务器
└─ Hypervisor
   ├─ 虚拟机 A：操作系统 + 应用
   └─ 虚拟机 B：操作系统 + 应用
```

### 3.2 容器

容器把应用、运行时和依赖打包在一起，但同一宿主环境中的容器共享操作系统内核。
容器通常启动快、体积小，适合部署服务化应用。

```text
操作系统或 Linux 虚拟环境
└─ 容器运行时
   ├─ 容器 A：应用 + 依赖
   └─ 容器 B：应用 + 依赖
```

Windows 上的 Docker Desktop 通常借助 WSL2 运行 Linux 容器。因此这里存在两层概念：

- WSL2 提供 Linux 内核运行环境；
- Docker 在该环境中创建和管理容器。

### 3.3 Kubernetes

Kubernetes（K8s）的本质是容器编排系统，而不是构建容器的工具。它负责把容器工作负载
调度到集群节点，并持续保证声明的期望状态。

```text
代码 → Dockerfile → 镜像 → 容器 → Kubernetes 编排
```

例如声明某服务需要三个副本，Kubernetes 会尽量保持三个可用实例。某实例崩溃或节点离线
时，它会重新创建或重新调度实例。

## 4. Docker 核心概念

### 4.1 镜像与容器

镜像是只读模板，容器是镜像运行后形成的进程实例。

```text
Dockerfile --构建--> Image --运行--> Container
```

同一个镜像可以创建多个容器。删除容器不会自动删除镜像；删除镜像也不能直接删除保存在
外部数据卷中的数据。

常用命令：

```powershell
docker image ls
docker container ls
docker container ls -a
docker logs <容器名>
docker exec -it <容器名> sh
```

### 4.2 Dockerfile

Dockerfile 描述如何制作镜像。常见指令包括：

| 指令 | 作用 |
| --- | --- |
| `FROM` | 指定基础镜像 |
| `WORKDIR` | 设置工作目录 |
| `COPY` | 将构建上下文中的文件复制进镜像 |
| `RUN` | 构建时执行命令 |
| `ENV` | 设置环境变量 |
| `EXPOSE` | 声明容器服务端口 |
| `ENTRYPOINT` / `CMD` | 设置容器启动命令 |

当前工程使用多阶段构建。例如 Java 服务先在 Maven 镜像中编译，再将 JAR 放入较小的 JRE
镜像；前端先在 Node.js 镜像中编译，再将静态资源放入 Nginx 镜像。这样可以减小最终镜像，
并避免把编译工具带入运行环境。

### 4.3 Docker Compose

Docker Compose 用一个 YAML 文件描述单机上的多个容器、网络、端口、数据卷和依赖关系。
它很适合本地开发、集成测试和中小规模单机部署。

常用命令：

```powershell
docker compose up -d --build
docker compose ps
docker compose logs -f
docker compose restart ai-server
docker compose stop
docker compose down
```

`docker compose down` 会删除 Compose 创建的容器和默认网络，但默认不会删除命名卷。
`docker compose down -v` 会连同数据卷一起删除，应谨慎使用。

## 5. 当前 Service Platform 架构

### 5.1 容器组成

当前 `compose.yaml` 定义七个独立服务：

| 服务 | 职责 | 主要技术 | 宿主机端口 |
| --- | --- | --- | --- |
| `mysql` | 统一关系型数据库 | MySQL 8.4 LTS | 3307 |
| `platform-server` | 平台、用户、权限与应用生命周期 API | Java 21、Spring Boot | 9090 |
| `ai-server` | AI 配置、模型调用与会话 API | Java 21、Spring Boot | 8081 |
| `task-server` | 任务定义、执行与日志 API | Java 21、Spring Boot | 8082 |
| `platform-web` | 平台管理页面 | React、TypeScript、Ant Design、Nginx | 9091 |
| `ai-web` | AI 管理页面 | React、TypeScript、Ant Design、Nginx | 9092 |
| `task-web` | Task 管理页面 | React、TypeScript、Ant Design、Nginx | 9093 |

逻辑关系如下：

```text
浏览器
├─ localhost:9091 → platform-web → platform-server ─┐
├─ localhost:9092 → ai-web       → ai-server       ├→ mysql
└─ localhost:9093 → task-web     → task-server     ┘
```

### 5.2 容器为什么能够互相访问

Compose 会为项目创建内部网络，并把服务名注册为内部 DNS 名。Java 服务使用下面的地址访问
MySQL：

```text
jdbc:mysql://mysql:3306/service_platform
```

其中 `mysql` 是 Compose 服务名。容器之间不能使用 `localhost` 互相访问，因为每个容器的
`localhost` 都只指向该容器自身。

### 5.3 Windows 为什么能够访问容器

Compose 的端口映射格式为：

```text
宿主机端口:容器端口
```

MySQL 使用 `3307:3306`，所以 Windows 上的 Navicat 可以连接：

```text
主机：127.0.0.1
端口：3307
数据库：service_platform
```

Docker Desktop 接收 `127.0.0.1:3307` 的连接，并转发到 MySQL 容器的 `3306`。

若只允许本机访问，可以将端口绑定写成：

```yaml
ports:
  - "127.0.0.1:3307:3306"
```

### 5.4 数据持久化

MySQL 数据存放在命名卷 `platform-mysql` 中，而不是只保存在容器可写层中。
容器重新创建后仍可挂载原数据卷。

```powershell
docker volume ls
docker volume inspect platform_platform-mysql
```

生产环境还必须建立独立备份策略。数据卷不是备份，它只能降低容器重建导致数据丢失的风险，
无法防止误删、数据损坏和磁盘故障。

## 6. 项目使用的应用技术

### 6.1 Java 21 与 Spring Boot

Java 21 是长期支持版本。Spring Boot 提供 Web 服务、配置加载、安全控制、数据库访问、
健康检查等基础能力。当前后端采用 Maven 管理依赖和构建生命周期。

典型构建过程：

```text
Java 源码 → Maven 编译与测试 → JAR → JRE 容器运行
```

### 6.2 React、TypeScript 与 Ant Design

- React 负责组件化页面和状态更新；
- TypeScript 为 JavaScript 增加静态类型检查；
- Ant Design 提供表单、表格、菜单、弹窗等企业级 UI 组件；
- Vite 提供开发服务器和前端构建；
- Nginx 在容器中提供编译后的静态文件。

Ant Design 是前端依赖库，不是独立后台服务，也不是独立容器。每个 Web 工程构建后形成一套
静态文件，并由对应 Nginx 容器提供访问。

### 6.3 MySQL 8.4 LTS

MySQL 8.4 属于长期支持系列，适合希望功能稳定、长期获取缺陷和安全修复的项目。
当前三个后端共用 `service_platform` 数据库，并以表名前缀和独立 Flyway 历史表区分模块。

### 6.4 Flyway

Flyway 用版本化 SQL 管理数据库结构变化。应用启动时检查迁移历史，并按顺序执行尚未应用的
脚本。这样可以避免依靠人工在不同环境中逐条执行建表或修改语句。

数据库迁移应遵循：

- 已发布并执行过的迁移文件不要修改；
- 新变化添加新版本迁移；
- 大表变更要评估锁表和执行时长；
- 上线前备份并验证回滚或修复方案。

### 6.5 JWT 与权限控制

当前平台由 Platform Server 统一签发 JWT，并通过 HttpOnly Cookie 或
`Authorization: Bearer` 传递身份。角色包括 `ADMIN`、`OPERATOR` 和 `VIEWER`。

权限系统通常包含：

```text
用户 → 角色 → 权限 → API/页面操作
```

前端隐藏按钮只能改善用户体验，真正的安全边界必须由后端接口鉴权保证。

## 7. 微服务相关知识

微服务将一个系统拆成多个可以独立开发、部署和扩缩容的服务。拆分边界通常围绕业务职责，
而不是单纯按照技术层拆分。

### 7.1 优点

- 服务可以独立发布和扩容；
- 故障影响范围更容易隔离；
- 团队可以按业务域分工；
- 不同服务可以选择合适的技术。

### 7.2 成本

- 网络调用会失败并产生延迟；
- 分布式事务和数据一致性更复杂；
- 日志、监控和问题定位难度上升；
- 配置、版本兼容和发布协调成本增加；
- 需要成熟的自动化与治理体系。

不要仅因为可以使用容器就把系统过度拆分。容器边界、服务边界和代码模块边界可以不同。

### 7.3 常见治理能力

| 能力 | 说明 | 常见实现 |
| --- | --- | --- |
| 服务发现 | 根据服务名找到实例 | Kubernetes Service、Nacos、Consul |
| 配置管理 | 集中管理不同环境配置 | ConfigMap、Secret、Nacos |
| API 网关 | 路由、认证、限流 | APISIX、Kong、Spring Cloud Gateway |
| 负载均衡 | 在多个实例间分发请求 | Kubernetes Service、Nginx、Envoy |
| 熔断降级 | 下游异常时保护系统 | Resilience4j、Sentinel |
| 消息通信 | 解耦异步流程 | Kafka、RabbitMQ、RocketMQ |

## 8. Kubernetes 核心对象

当项目从单机 Compose 迁移到 Kubernetes 时，通常会使用以下对象：

| 对象 | 作用 |
| --- | --- |
| Pod | Kubernetes 最小调度单位，包含一个或多个容器 |
| Deployment | 管理无状态应用副本和滚动更新 |
| StatefulSet | 管理需要稳定身份和存储的有状态实例 |
| Service | 为一组 Pod 提供稳定访问地址和负载均衡 |
| Ingress | 管理来自集群外部的 HTTP/HTTPS 路由 |
| ConfigMap | 保存非敏感配置 |
| Secret | 保存密码、令牌、证书等敏感配置 |
| PersistentVolume | 描述持久化存储资源 |
| PersistentVolumeClaim | 应用对持久化存储的申请 |
| Job / CronJob | 执行一次性或定时任务 |
| Namespace | 对资源进行逻辑隔离和配额管理 |

Compose 与 Kubernetes 的粗略对应关系：

| Compose | Kubernetes |
| --- | --- |
| service | Deployment/StatefulSet + Service |
| ports | Service + Ingress |
| environment | ConfigMap + Secret |
| volume | PVC + StorageClass |
| healthcheck | readinessProbe + livenessProbe |
| restart policy | 控制器维护期望副本 |

Kubernetes 不直接替代镜像构建。镜像通常由 CI 系统构建并推送到 Harbor 等仓库，然后由
Kubernetes 节点拉取和运行。

## 9. 网络体系

云平台网络通常包含以下层次：

```text
用户
  ↓ DNS / CDN / 防火墙
负载均衡器
  ↓
Ingress 或 API 网关
  ↓
Service
  ↓
Pod / 容器
  ↓
数据库、缓存、消息队列
```

### 9.1 常见概念

- DNS：把域名解析成 IP 或服务地址；
- 负载均衡：把请求分配给多个实例；
- Ingress：将外部 HTTP/HTTPS 请求路由到集群服务；
- CNI：实现 Kubernetes Pod 网络，如 Calico、Cilium；
- NetworkPolicy：限制哪些 Pod 可以互相访问；
- TLS：加密网络传输并验证服务身份；
- API 网关：在入口处实现认证、路由、限流、审计等能力。

### 9.2 端口暴露原则

只有真正需要被外部访问的服务才应发布宿主机端口。数据库、Redis 和消息队列在生产环境中
通常只允许内网或特定应用访问，不应直接暴露到公网。

## 10. 存储体系

云平台常见三类存储：

| 类型 | 访问形式 | 典型用途 | 常见技术 |
| --- | --- | --- | --- |
| 块存储 | 类似独立磁盘 | 数据库磁盘、虚拟机磁盘 | 云盘、Ceph RBD |
| 文件存储 | 目录和文件共享 | 共享文件、模型、报表 | NFS、CephFS |
| 对象存储 | HTTP API + Bucket/Object | 图片、附件、备份、归档 | S3、MinIO、Ceph RGW |

数据库数据一般使用高可靠块存储；用户上传文件更适合对象存储，不宜长期放在应用容器本地目录。

## 11. 数据与中间件

### 11.1 关系型数据库

MySQL、PostgreSQL 适合需要事务、约束和复杂查询的结构化数据。生产使用时需关注：

- 主从复制或高可用；
- 定期全量与增量备份；
- 恢复演练；
- 慢查询和索引；
- 连接池上限；
- 数据容量和归档；
- 版本升级兼容性。

### 11.2 Redis

Redis 常用于缓存、会话、分布式锁、计数器和短期状态。缓存不能被当作天然可靠的数据源，
应考虑缓存穿透、击穿、雪崩、过期和一致性问题。

### 11.3 消息队列

Kafka、RabbitMQ、RocketMQ 用于异步解耦、削峰填谷和事件驱动。消息系统需要明确：

- 至少一次、至多一次或恰好一次语义；
- 消费失败重试和死信策略；
- 幂等处理；
- 消息顺序；
- 积压监控；
- 消息保留周期。

## 12. DevOps 与持续交付

典型交付链路：

```text
提交代码
  ↓
CI：编译、单元测试、静态检查
  ↓
构建版本化镜像
  ↓
推送到 Harbor
  ↓
CD/GitOps：更新部署声明
  ↓
Kubernetes 滚动发布
  ↓
健康检查、指标验证、失败回滚
```

常见工具：

- GitLab CI、Jenkins、GitHub Actions：持续集成；
- Harbor：私有镜像仓库、镜像扫描和权限管理；
- Helm：Kubernetes 应用模板与发布包；
- Argo CD、Flux：以 Git 中的声明驱动部署；
- Terraform：创建云资源；
- Ansible：配置机器和自动化运维。

镜像应使用明确版本，例如 `platform/platform-server:1.2.3`，不建议生产环境只使用
`latest`。同一版本镜像应保持不可变，避免同一个标签在不同时间代表不同内容。

## 13. 可观测性

可观测性主要由指标、日志和链路三部分组成：

| 信号 | 回答的问题 | 常见技术 |
| --- | --- | --- |
| Metrics | 系统是否健康、性能是否异常 | Prometheus、Grafana |
| Logs | 某个时间点具体发生了什么 | Loki、ELK、OpenSearch |
| Traces | 一次请求经过哪些服务、耗时在哪里 | OpenTelemetry、Jaeger、SkyWalking |

### 13.1 建议监控的指标

- 请求量、错误率、响应耗时；
- JVM 堆内存、GC、线程数；
- 容器 CPU、内存、重启次数；
- MySQL 连接数、慢查询、磁盘空间；
- 队列积压和消费失败；
- 登录失败、权限拒绝和敏感操作数量。

### 13.2 健康检查

- 存活检查回答“进程是否需要重启”；
- 就绪检查回答“实例是否可以接收流量”；
- 启动检查适合启动耗时较长的应用。

不要把所有下游依赖都放入存活检查，否则数据库短暂异常可能导致所有业务容器反复重启。

## 14. 安全体系

### 14.1 身份与权限

- 用户认证确认“你是谁”；
- 权限控制确认“你能做什么”；
- 服务身份确认“哪个服务在调用”；
- 最小权限原则要求账号只拥有完成任务所需权限。

### 14.2 密钥管理

密码、JWT 密钥、数据库凭证和 API Token 不应写入代码或提交到 Git。开发环境可使用被忽略的
`.env`，生产环境应使用 Kubernetes Secret、Vault 或云厂商密钥管理服务。

需要注意：Kubernetes Secret 默认主要是 Base64 编码，不等同于加密。仍需结合访问控制、
etcd 加密和外部密钥管理方案。

### 14.3 镜像与供应链安全

- 使用可信基础镜像；
- 固定并定期更新版本；
- 扫描已知漏洞；
- 生成 SBOM；
- 尽量以非 root 用户运行；
- 删除不必要工具和依赖；
- 对镜像签名并验证来源；
- 保护 CI 凭证和镜像仓库权限。

### 14.4 网络安全

- 外部访问统一经过 HTTPS；
- 数据库和中间件不直接暴露公网；
- 通过防火墙和 NetworkPolicy 限制访问；
- 管理接口与业务接口适当隔离；
- 对登录、配置变更、任务执行建立审计日志。

## 15. 高可用、扩缩容与灾难恢复

### 15.1 高可用

高可用不是简单地启动多个容器。还需要：

- 多个实例分布在不同节点；
- 入口负载均衡；
- 数据库高可用；
- 无单点存储；
- 健康检查和自动故障转移；
- 配置、会话和文件不能只依赖某个实例本地状态。

### 15.2 扩缩容

无状态 Web/API 服务通常可以水平增加实例。数据库和带本地状态的任务服务扩容更复杂，
需要处理数据一致性、任务抢占、分布式锁和并发限制。

### 15.3 备份与恢复

应定义：

- RPO：最多能接受丢失多长时间的数据；
- RTO：故障后多长时间内必须恢复；
- 备份频率、保留周期和异地副本；
- 恢复操作步骤与定期演练。

没有经过恢复验证的备份不能被视为可靠备份。

## 16. 当前项目的建议演进路线

### 阶段一：完善单机 Compose 环境

- 为所有服务增加明确健康检查；
- 固定关键基础镜像版本；
- 将端口按需绑定到 `127.0.0.1`；
- 建立 MySQL 备份和恢复脚本；
- 统一结构化日志和请求 ID；
- 确保 `.env` 不提交到版本库；
- 为构建和启动增加自动验证。

### 阶段二：建立镜像与 CI

- 搭建 Harbor；
- CI 执行 Maven、前端构建和测试；
- 为三个后端和三个前端分别构建版本化镜像；
- 执行依赖和镜像漏洞扫描；
- 自动生成构建制品与版本记录。

### 阶段三：迁移到 Kubernetes

- Web/API 使用 Deployment；
- 使用 Service 提供集群内部访问；
- 使用 Ingress 或 API 网关提供统一入口；
- 配置放入 ConfigMap，敏感信息放入 Secret；
- 为服务配置资源 request/limit 和健康探针；
- 使用 PVC 或外部数据库承载持久数据；
- 使用 Helm 管理开发、测试和生产环境差异。

### 阶段四：完善生产治理

- Prometheus + Grafana 监控告警；
- Loki/ELK 集中日志；
- OpenTelemetry 链路追踪；
- Argo CD 实现 GitOps；
- 自动扩缩容、滚动发布与回滚；
- 数据库高可用、备份与灾备；
- SSO、细粒度 RBAC、密钥管理和审计。

## 17. 常用排查命令

### 17.1 Docker 状态

```powershell
docker version
docker info
docker image ls
docker container ls -a
docker compose ps
```

### 17.2 构建和启动

```powershell
cd D:\dev\code\paas\platform
docker compose build
docker compose up -d
docker compose ps
```

### 17.3 日志

```powershell
docker compose logs --tail=200
docker compose logs -f platform-server
docker compose logs -f mysql
```

### 17.4 MySQL

```powershell
docker compose exec mysql mysql -uroot -p
```

进入 MySQL 后：

```sql
SHOW DATABASES;
USE service_platform;
SHOW TABLES;
SELECT VERSION();
```

### 17.5 网络

```powershell
Test-NetConnection 127.0.0.1 -Port 3307
Test-NetConnection 127.0.0.1 -Port 9091
docker network ls
docker compose exec platform-server getent hosts mysql
```

### 17.6 Docker Hub 拉取失败

若出现 `failed to fetch anonymous token` 或连接 `auth.docker.io:443` 超时，说明问题发生在
基础镜像下载阶段，通常与网络、DNS 或代理有关，而不是 Java/前端源码编译错误。可以先根据
Dockerfile 的 `FROM` 指令逐个拉取基础镜像，再重新构建。

```powershell
docker pull maven:3.9-eclipse-temurin-21
docker pull eclipse-temurin:21-jre
docker pull docker:27-cli
docker pull node:22-alpine
docker pull nginx:1.27-alpine
docker pull mysql:8.4
```

## 18. 推荐学习顺序

1. Linux 基础：文件、权限、进程、端口、日志和 Shell；
2. 网络基础：TCP/IP、DNS、HTTP/HTTPS、代理和负载均衡；
3. Docker：镜像、容器、Dockerfile、网络、数据卷；
4. Docker Compose：多服务编排、环境变量和健康检查；
5. Java/Spring Boot 与 React 前后端部署；
6. MySQL：SQL、索引、事务、备份与权限；
7. CI/CD：自动测试、镜像构建和版本管理；
8. Kubernetes：Pod、Deployment、Service、Ingress、配置和存储；
9. 可观测性：指标、日志、链路和告警；
10. 安全、高可用、容量规划与灾难恢复。

## 19. 核心结论

- Docker 负责构建镜像和运行容器；
- Docker Compose 负责在单台机器上统一管理多个容器；
- Kubernetes 负责在集群中调度和治理容器化应用；
- 容器不是虚拟机，它通常共享所在 Linux 环境的内核；
- 一个系统可以由多个独立容器组成，容器之间通过网络和服务名通信；
- 数据库必须使用持久化存储，并建立独立备份与恢复能力；
- 云平台的核心价值不仅是容器化，还包括资源调度、网络、存储、发布、监控、安全和治理；
- 当前 `platform` 已具备多服务容器化基础，可逐步向 CI、镜像仓库、Kubernetes 和
  完整可观测性演进。
