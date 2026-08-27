# Integration working notes

## Authoritative sources

- AI API routes, provider base URL behavior and request contracts: existing production-style
  source under `D:\dev\code\ai\personal\ai-console\ai-console-server`.
- AI database schema: existing Flyway migrations V1-V4 in that project. The tables already
  use the `ai_` prefix.
- Task callable classes and SDK usage: existing source under
  `D:\dev\code\lang\java\task-executor`; only classes with an explicit public static
  `main(String[])` are eligible for registration.
- Huifu SDK coordinate: existing `task-executor/pom.xml`,
  `com.huifu.bspay.sdk:dg-java-sdk:3.0.17`.

## Preserved behavior

- The original `ai-console` and `task-executor` directories remain unchanged.
- AI `/api/v1` contracts and provider downstream URL construction are copied without routing
  changes.
- Task business classes, configs, SDK calls, retry rules, request fields and Excel behavior are
  copied without refactoring. The new server adds an outer whitelist and execution-history layer.
- No table name is converted into an internal HTTP service name.

## New platform-owned contracts

- Database: `service_platform`.
- Table namespaces: `platform_`, `ai_`, `task_`.
- Platform API: `/api/applications`.
- Task management API: `/api/v1/tasks` and `/api/v1/executions`.

## Task page-input sources

- Merchant-id list inputs come from the existing `BaseController.execute(String[])` and
  `ExcelRecordService.resolveHuifuIds` behavior.
- `batch` / `remaining` modes come from the existing HeZhao and Yuanzu main methods.
- Excel/image inputs come only from the existing `TASK_INPUT_PATH`, `TASK_AUX_INPUT_PATH`
  and `TASK_OUTPUT_DIR` integration points.
- MCC form keys come from the existing `ModifyMerchantMccPostTask.RequestParams` switch.
- Huifu JS-pay, Huifu business-open and Buluke refund fields come from the exact request
  setters and constants already present in those three task classes.
- Page input is an outer adapter. Calling a task main method without page context retains its
  existing defaults and argument behavior.

## Console authentication authorization

- User explicitly authorized console login, logout and permission control on 2026-08-20.
- New platform-owned endpoints use `/api/auth/**` and `/api/admin/**`; they do not replace or
  redirect an existing company-internal service.
- Roles are platform-local: `ADMIN`, `OPERATOR`, `VIEWER`. Permissions are embedded into a
  platform-issued signed JWT and checked independently by platform-server, ai-server and
  task-server.
- Existing AI and Task business routes and downstream destinations remain unchanged; only
  access to the local management APIs becomes authenticated.

## Compose platform-web origin

- Authoritative source: `apps/platform/platform-web/nginx.conf` proxies `/api/` to the existing
  `platform-server:9090` endpoint, while `compose.yaml` publishes platform-web on host port 9091.
- Browser POST requests retain `Origin: http://localhost:9091`; platform-server must allow this
  Compose web origin in addition to the existing Vite development origins.
- This CORS addition does not change an endpoint, request body, downstream destination, JWT
  contract, or the AI and Task business routes.

## Huifu merchant key ownership

- User-confirmed configuration ownership: the merchant profile contains the merchant public/private keys and the Huifu public key.
- Authoritative SDK source: `dg-java-sdk-3.0.40-sources.jar`, `MerConfig.java`, defines `rsa_private_key` as the merchant signing private key and `rsa_public_key` as the public key used to verify Huifu responses.
- Platform-facing canonical names are therefore `merchant_public_key`, `merchant_private_key`, and `huifu_public_key`; the SDK setter names and downstream Huifu calls remain unchanged.
- User explicitly requested a fixed merchant common-configuration form on 2026-08-25 so operators never guess field names or capitalization. The canonical field set verified in the existing HUIFU profile and task adapters is `sys_id`, `huifu_id`, `product_id`, `upper_huifu_id`, `huifu_public_key`, `merchant_public_key`, and `merchant_private_key`.
- Merchant create/update APIs must reject unknown or differently-cased configuration keys. This validation changes only platform-local configuration storage and leaves SDK setter names, request fields and downstream calls unchanged.

## Task workflow foundation

- User-authorized first workflow set: merchant basic-data modification plus application query, merchant business opening plus application query, and merchant business-opening modification plus application query. Picture upload is optional; future payment flows must reuse the engine without inventing payment calls.
- User-authorized waiting behavior: application queries that remain processing support configured interval polling and an explicit manual query trigger.
- Authoritative call definitions: Huifu `dg-java-sdk` 3.0.40 request classes `V2MerchantBasicdataModifyRequest`, `V2MerchantBusiOpenRequest`, `V2MerchantBusiModifyRequest`, and `V2MerchantBasicdataStatusQueryRequest` define these four callable operations and request fields.
- Existing executor behavior in `AbstractMerchantBatchTask` treats `apply_no` as the submission/query correlation value and `apply_status=P` as pending. The workflow engine preserves those semantics and does not add a new endpoint or response code.
- Existing `main()` tasks stay available through the legacy adapter; workflow execution is introduced as a separate path so unrelated DJI, Cotti, Hema, Yuanzu, Hezhao, Quhulian, Huifu, and Buluke task behavior remains unchanged.

## Task package and atomic execution output

- User explicitly requested removing `lab` from the current Task project package on 2026-08-25; the canonical package root is `com.platform.task`, already established by `TaskApplication`.
- Existing Huifu picture upload remains `BasePayClient.upload(request, file)` through `MerchantApiTaskSupport`; its endpoint, SDK request type, configuration and request fields are unchanged.
- `MerchantApiTaskSupport.upload` already records the exact SDK response through `TaskExecutionContext.recordOutput`. The compatibility `main()` adapter currently discards that captured value, so atomic executions report success without exposing their result.
- The isolated fix persists the captured output on the execution record and returns it through the existing execution-history API; it does not synthesize a response or change any downstream call.
- Authoritative runtime evidence from the successful 2026-08-25 15:00:40 picture upload shows the SDK response shape as an outer map whose `data` value is a JSON string containing `data.file_id`. Response normalization may expose that exact value as top-level `file_id` while retaining the original SDK response.

## Application display order

- User explicitly requested an application-card sort code and Platform first on 2026-08-21.
- The authoritative application definitions remain `registry/applications.yml` and
  `registry/applications.compose.yml`; they define `sortOrder` as Platform 10, AI 20, Task 30.
- Sorting changes only `/api/applications` list order and catalog persistence. Application IDs,
  lifecycle commands, links, components, and downstream routes remain unchanged.

## Task catalog wording

- User explicitly requested current-capability wording without emphasizing legacy compatibility
  on 2026-08-21.
- Only the Task application description in both authoritative registry variants changes; Task
  classes, Java `main()` invocation, request contracts, lifecycle commands, and routes remain unchanged.

## DJI task platform configuration adaptation

- User explicitly requested adapting the previous DJI task code under
  `D:\dev\code\lang\java\task-executor\src\main\java\com\lab\taskexecutor\controller\dji`
  to the current Task platform on 2026-08-26.
- The previous DJI classes are the authoritative source for the existing Huifu SDK request types,
  request fields, call variants, endpoints selected by the SDK, and direct `main()` fallback values.
- The current platform injects the selected merchant profile through `TaskExecutionContext` using
  the canonical `product_id`, `sys_id`, `upper_huifu_id`, `merchant_private_key`, and
  `huifu_public_key` keys. DJI tasks may consume those values without changing their Huifu request
  types, request payload defaults, call order, retry behavior, or direct `main()` fallback behavior.
- The adaptation is isolated to the DJI package; shared task helpers and unrelated merchant tasks
  remain unchanged.
