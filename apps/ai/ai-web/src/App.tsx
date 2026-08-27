import {
  useEffect,
  useRef,
  useState,
  type KeyboardEvent,
  type ReactNode,
} from "react";
import {
  App as AntApp,
  Button,
  Card,
  Col,
  Drawer,
  Form,
  Input,
  InputNumber,
  Layout,
  Menu,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
} from "antd";
import {
  ApiOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  BarChartOutlined,
  CheckOutlined,
  DatabaseOutlined,
  MenuFoldOutlined,
  MenuOutlined,
  MenuUnfoldOutlined,
  MessageOutlined,
  PlusOutlined,
  RobotOutlined,
  SendOutlined,
  SyncOutlined,
} from "@ant-design/icons";
import { api, type Model, type Provider, type Stats } from "./api";
import { can, login, me, saveSession, saveUser, storedUser, type Session, type UserSession } from "./auth";

const { Header, Sider, Content } = Layout;

function ConversationHistoryIcon() {
  return (
    <svg className="chat-toolbar-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M6.5 18.2 3.8 20l.7-3.3A7.8 7.8 0 0 1 3 12c0-4.4 4-8 9-8s9 3.6 9 8-4 8-9 8c-2 0-3.9-.6-5.5-1.8Z" />
      <path d="M12 8.2v4.1l2.7 1.6" />
    </svg>
  );
}

function NewConversationIcon() {
  return (
    <svg className="chat-toolbar-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M6.5 18.2 3.8 20l.7-3.3A7.8 7.8 0 0 1 3 12c0-4.4 4-8 9-8s9 3.6 9 8-4 8-9 8c-2 0-3.9-.6-5.5-1.8Z" />
      <path d="M12 8.5v7M8.5 12h7" />
    </svg>
  );
}

function CopyIcon() {
  return (
    <svg
      className="copy-icon"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <rect x="8" y="7" width="11" height="13" rx="2" />
      <path d="M16 7V5a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h2" />
    </svg>
  );
}

function ResendIcon() {
  return (
    <svg className="resend-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M19 8.5A8 8 0 1 0 20 14" />
      <path d="M19 4.5v4h-4" />
    </svg>
  );
}

function MessageContent({
  content,
  onCopy,
}: {
  content: string;
  onCopy: (text: string) => void;
}) {
  const parts: ReactNode[] = [];
  const pattern = /```([\w#+.-]*)\s*\r?\n([\s\S]*?)```/g;
  let cursor = 0;
  let match: RegExpExecArray | null;
  let key = 0;
  const text = (value: string) => (
    <span className="message-text">
      {value.split(/(`[^`\n]+`)/g).map((part, index) =>
        part.startsWith("`") && part.endsWith("`") ? (
          <code className="inline-code" key={index}>
            {part.slice(1, -1)}
          </code>
        ) : (
          part
        ),
      )}
    </span>
  );
  while ((match = pattern.exec(content)) !== null) {
    if (match.index > cursor)
      parts.push(
        <span key={key++}>{text(content.slice(cursor, match.index))}</span>,
      );
    const code = match[2].replace(/\s+$/, "");
    const language = match[1] || "text";
    parts.push(
      <div className="code-editor" key={key++}>
        <div className="code-editor-header">
          <span>{language}</span>
          <Button
            type="text"
            size="small"
            icon={<CopyIcon />}
            aria-label="复制代码"
            onClick={() => onCopy(code)}
          >
            复制代码
          </Button>
        </div>
        <pre>
          <code className={`language-${language}`}>{code}</code>
        </pre>
      </div>,
    );
    cursor = pattern.lastIndex;
  }
  if (cursor < content.length)
    parts.push(<span key={key}>{text(content.slice(cursor))}</span>);
  return <>{parts}</>;
}

function Dashboard() {
  const [data, setData] = useState<Stats>();
  const [providers, setProviders] = useState<Provider[]>([]);
  const [models, setModels] = useState<Model[]>([]);
  const [providerId, setProviderId] = useState<number>();
  const [modelId, setModelId] = useState<number>();
  useEffect(() => {
    Promise.all([api.get("/providers"), api.get("/models")]).then(([p, m]) => {
      setProviders(p.data);
      setModels(m.data);
    });
  }, []);
  useEffect(() => {
    const params = modelId
      ? { modelId }
      : providerId
        ? { providerId }
        : undefined;
    api.get("/stats/overview", { params }).then((r) => setData(r.data));
  }, [providerId, modelId]);
  const cards = [
    ["总调用", data?.totalCalls ?? 0],
    ["成功率", data?.successRate ?? 0, "%"],
    ["输入 Token", data?.inputTokens ?? 0],
    ["输出 Token", data?.outputTokens ?? 0],
    ["平均耗时", data?.averageDurationMs ?? 0, "ms"],
    ["预估费用", data?.estimatedCost ?? 0],
  ];
  const providerModels = models.filter((m) => m.providerId === providerId);
  const selectProvider = (value?: number) => {
    setProviderId(value);
    setModelId(undefined);
  };
  return (
    <>
      <Space className="page-title" wrap>
        <Select
          aria-label="服务商筛选"
          value={providerId}
          onChange={selectProvider}
          showSearch
          optionFilterProp="label"
          allowClear
          placeholder="全部服务商"
          options={providers.map((p) => ({ value: p.id, label: p.name }))}
          style={{ minWidth: 220 }}
        />
        <Select
          aria-label="模型筛选"
          value={modelId}
          onChange={setModelId}
          showSearch
          optionFilterProp="label"
          allowClear
          disabled={!providerId}
          placeholder={providerId ? "输入或选择模型" : "请先选择服务商"}
          options={providerModels.map((m) => ({
            value: m.id,
            label: `${m.displayName}（${m.code}）`,
          }))}
          style={{ minWidth: 220 }}
        />
      </Space>
      <Row gutter={[16, 16]}>
        {cards.map(([title, value, suffix]) => (
          <Col xs={24} md={12} xl={8} key={String(title)}>
            <Card>
              <Statistic
                title={title}
                value={value}
                suffix={suffix}
                precision={String(title).includes("率") ? 2 : undefined}
              />
            </Card>
          </Col>
        ))}
      </Row>
    </>
  );
}

function Providers() {
  const { message } = AntApp.useApp();
  const [items, setItems] = useState<Provider[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Provider>();
  const [movingId, setMovingId] = useState<number>();
  const [form] = Form.useForm();
  const load = () => api.get("/providers").then((r) => setItems(r.data));
  useEffect(() => {
    load();
  }, []);
  const edit = (p?: Provider) => {
    setEditing(p);
    form.setFieldsValue(p ? { ...p, apiKey: "" } : { enabled: true, sortOrder: 0 });
    setOpen(true);
  };
  const save = async () => {
    try {
      const v = await form.validateFields();
      if (editing) await api.put(`/providers/${editing.id}`, v);
      else await api.post("/providers", v);
      message.success("已保存");
      setOpen(false);
      load();
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e.response?.data?.message || e.message || "保存失败");
    }
  };
  const move = async (id: number, adjacentId: number) => {
    setMovingId(id);
    try {
      await api.post(`/providers/${id}/move`, { adjacentId });
      await load();
    } catch (e: any) {
      message.error(e.response?.data?.message || e.message || "排序失败");
    } finally {
      setMovingId(undefined);
    }
  };
  return (
    <>
      <Space className="page-title">
        <Tooltip title="新增服务商">
          <Button
            className="page-action-icon-button"
            type="text"
            shape="circle"
            icon={<PlusOutlined />}
            disabled={!can("ai:write")}
            aria-label="新增服务商"
            onClick={() => edit()}
          />
        </Tooltip>
      </Space>
      <Table
        rowKey="id"
        dataSource={items}
        columns={[
          { title: "名称", dataIndex: "name" },
          { title: "Base URL", dataIndex: "baseUrl" },
          {
            title: "排序",
            width: 130,
            render: (_, r) => {
              const index = items.findIndex((item) => item.id === r.id);
              return (
                <Space size={2}>
                  <span>{r.sortOrder}</span>
                  <Button
                    type="text"
                    size="small"
                    icon={<ArrowUpOutlined />}
                    disabled={!can("ai:write") || index <= 0 || movingId !== undefined}
                    loading={movingId === r.id}
                    aria-label="服务商上移"
                    onClick={() => void move(r.id, items[index - 1].id)}
                  />
                  <Button
                    type="text"
                    size="small"
                    icon={<ArrowDownOutlined />}
                    disabled={!can("ai:write") || index < 0 || index >= items.length - 1 || movingId !== undefined}
                    aria-label="服务商下移"
                    onClick={() => void move(r.id, items[index + 1].id)}
                  />
                </Space>
              );
            },
          },
          {
            title: "密钥",
            render: (_, r) => (
              <Tag color={r.hasApiKey ? "green" : "default"}>
                {r.hasApiKey ? "已配置" : "未配置"}
              </Tag>
            ),
          },
          {
            title: "状态",
            render: (_, r) => (
              <Tag color={r.enabled ? "blue" : "default"}>
                {r.enabled ? "启用" : "停用"}
              </Tag>
            ),
          },
          {
            title: "操作",
            render: (_, r) => (
              <Space>
                <Button type="link" disabled={!can("ai:write")} onClick={() => edit(r)}>
                  编辑
                </Button>
                <Popconfirm
                  title="确认删除？"
                  onConfirm={() => api.delete(`/providers/${r.id}`).then(load)}
                >
                  <Button type="link" danger disabled={!can("ai:write")}>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? "编辑服务商" : "新增服务商"}
        open={open}
        onOk={save}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="baseUrl"
            label="Base URL"
            rules={[{ required: true }, { type: "url" }]}
          >
            <Input placeholder="http://localhost:11434/v1" />
          </Form.Item>
          <Form.Item
            name="apiKey"
            label="API Key"
            help={editing ? "留空表示保留原密钥" : ""}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序值" help="数值越小越靠前">
            <InputNumber precision={0} style={{ width: "100%" }} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

function Models() {
  const { message } = AntApp.useApp();
  const [items, setItems] = useState<Model[]>([]);
  const [providers, setProviders] = useState<Provider[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Model>();
  const [movingId, setMovingId] = useState<number>();
  const [syncing, setSyncing] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [providerFilter, setProviderFilter] = useState<number>();
  const [statusFilter, setStatusFilter] = useState<boolean>();
  const [priceFilter, setPriceFilter] = useState<"free" | "paid">();
  const [lifecycleFilter, setLifecycleFilter] = useState<
    "new" | "recent" | "historical" | "latest" | "preview" | "expiring"
  >();
  const [form] = Form.useForm();
  const load = () =>
    Promise.all([api.get("/models"), api.get("/providers")]).then(([m, p]) => {
      setItems(m.data);
      setProviders(p.data);
    });
  useEffect(() => {
    load();
  }, []);
  const edit = (m?: Model) => {
    setEditing(m);
    form.setFieldsValue(
      m ?? { enabled: true, free: true, sortOrder: 0, inputPricePerMillion: 0, outputPricePerMillion: 0 },
    );
    setOpen(true);
  };
  const save = async () => {
    try {
      const v = await form.validateFields();
      editing
        ? await api.put(`/models/${editing.id}`, v)
        : await api.post("/models", v);
      message.success("已保存");
      setOpen(false);
      load();
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e.response?.data?.message || e.message || "保存失败");
    }
  };
  const syncOpenRouter = async () => {
    const provider = providers.find((p) => {
      try {
        const url = new URL(p.baseUrl);
        return (
          url.protocol === "https:" &&
          url.hostname === "openrouter.ai" &&
          url.pathname.replace(/\/+$/, "") === "/api/v1"
        );
      } catch {
        return false;
      }
    });
    if (!provider) {
      message.warning(
        "请先添加 Base URL 为 https://openrouter.ai/api/v1 的服务商",
      );
      return;
    }
    setSyncing(true);
    try {
      const r = await api.post(`/providers/${provider.id}/models/sync`);
      message.success(
        `同步完成：新增 ${r.data.created}，更新 ${r.data.updated}`,
      );
      await load();
    } catch (e: any) {
      message.error(e.response?.data?.message || e.message || "同步失败");
    } finally {
      setSyncing(false);
    }
  };
  const isFree = (m: Model) => m.free;
  const ageDays = (m: Model) =>
    m.remoteCreatedAt
      ? Math.floor(
          (Date.now() - new Date(`${m.remoteCreatedAt}Z`).getTime()) / 86400000,
        )
      : undefined;
  const matchesLifecycle = (m: Model) => {
    const age = ageDays(m);
    if (lifecycleFilter === "new") return age !== undefined && age <= 30;
    if (lifecycleFilter === "recent")
      return age !== undefined && age > 30 && age <= 180;
    if (lifecycleFilter === "historical") return age !== undefined && age > 180;
    if (lifecycleFilter === "latest") return m.code.startsWith("~");
    if (lifecycleFilter === "preview")
      return /preview/i.test(`${m.code} ${m.displayName}`);
    if (lifecycleFilter === "expiring") return Boolean(m.expirationDate);
    return true;
  };
  const lifecycleTags = (m: Model) => {
    const tags: ReactNode[] = [];
    const age = ageDays(m);
    if (m.code.startsWith("~"))
      tags.push(
        <Tag color="blue" key="latest">
          动态最新
        </Tag>,
      );
    else if (age !== undefined && age <= 30)
      tags.push(
        <Tag color="green" key="new">
          最新上架
        </Tag>,
      );
    else if (age !== undefined && age <= 180)
      tags.push(
        <Tag color="cyan" key="recent">
          较新
        </Tag>,
      );
    else if (age !== undefined) tags.push(<Tag key="historical">历史</Tag>);
    else tags.push(<Tag key="manual">手动配置</Tag>);
    if (/preview/i.test(`${m.code} ${m.displayName}`))
      tags.push(
        <Tag color="purple" key="preview">
          Preview
        </Tag>,
      );
    if (m.expirationDate)
      tags.push(
        <Tag color="red" key="expiring">
          下线 {m.expirationDate}
        </Tag>,
      );
    return (
      <Space size={[0, 4]} wrap>
        {tags}
      </Space>
    );
  };
  const filteredItems = items.filter((m) => {
    const q = keyword.trim().toLowerCase();
    return (
      (!q ||
        m.displayName.toLowerCase().includes(q) ||
        m.code.toLowerCase().includes(q)) &&
      (providerFilter === undefined || m.providerId === providerFilter) &&
      (statusFilter === undefined || m.enabled === statusFilter) &&
      (priceFilter === undefined || (priceFilter === "free") === isFree(m)) &&
      matchesLifecycle(m)
    );
  });
  const toggle = async (m: Model, enabled: boolean) => {
    try {
      await api.put(`/models/${m.id}`, {
        providerId: m.providerId,
        code: m.code,
        displayName: m.displayName,
        enabled,
        inputPricePerMillion: m.inputPricePerMillion,
        outputPricePerMillion: m.outputPricePerMillion,
        sortOrder: m.sortOrder,
        free: m.free,
      });
      setItems((current) =>
        current.map((item) => (item.id === m.id ? { ...item, enabled } : item)),
      );
    } catch (e: any) {
      message.error(e.response?.data?.message || e.message || "状态更新失败");
    }
  };
  const move = async (id: number, adjacentId: number) => {
    setMovingId(id);
    try {
      await api.post(`/models/${id}/move`, { adjacentId });
      await load();
    } catch (e: any) {
      message.error(e.response?.data?.message || e.message || "排序失败");
    } finally {
      setMovingId(undefined);
    }
  };
  return (
    <>
      <Space className="page-title">
        <Tooltip title="一键同步 OpenRouter">
          <Button
            className="page-action-icon-button"
            type="text"
            shape="circle"
            icon={<SyncOutlined />}
            loading={syncing}
            disabled={!can("ai:write")}
            aria-label="一键同步 OpenRouter"
            onClick={syncOpenRouter}
          />
        </Tooltip>
        <Tooltip title="新增模型">
          <Button
            className="page-action-icon-button"
            type="text"
            shape="circle"
            icon={<PlusOutlined />}
            disabled={!can("ai:write")}
            aria-label="新增模型"
            onClick={() => edit()}
          />
        </Tooltip>
      </Space>
      <Space wrap className="model-filters">
        <Input.Search
          allowClear
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="搜索名称或模型编码"
          style={{ width: 260 }}
        />
        <Select
          allowClear
          value={providerFilter}
          onChange={setProviderFilter}
          showSearch
          optionFilterProp="label"
          placeholder="全部服务商"
          options={providers.map((p) => ({ value: p.id, label: p.name }))}
          style={{ width: 180 }}
        />
        <Select
          allowClear
          value={statusFilter}
          onChange={setStatusFilter}
          showSearch
          optionFilterProp="label"
          placeholder="全部状态"
          options={[
            { value: true, label: "已启用" },
            { value: false, label: "已停用" },
          ]}
          style={{ width: 140 }}
        />
        <Select
          allowClear
          value={priceFilter}
          onChange={setPriceFilter}
          showSearch
          optionFilterProp="label"
          placeholder="全部类型"
          options={[
            { value: "free", label: "免费模型" },
            { value: "paid", label: "付费模型" },
          ]}
          style={{ width: 140 }}
        />
        <Select
          allowClear
          value={lifecycleFilter}
          showSearch
          optionFilterProp="label"
          onChange={(value) => {
            setLifecycleFilter(value);
            void load();
          }}
          placeholder="全部生命周期"
          options={[
            { value: "new", label: "最新上架（30天）" },
            { value: "recent", label: "较新（180天）" },
            { value: "historical", label: "历史模型" },
            { value: "latest", label: "动态最新版" },
            { value: "preview", label: "Preview" },
            { value: "expiring", label: "有下线日期" },
          ]}
          style={{ width: 180 }}
        />
      </Space>
      <Table
        rowKey="id"
        dataSource={filteredItems}
        scroll={{ x: 1500 }}
        pagination={{
          defaultPageSize: 20,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 个模型`,
        }}
        columns={[
          { title: "显示名称", dataIndex: "displayName", width: 240 },
          { title: "模型编码", dataIndex: "code", width: 280 },
          {
            title: "规范版本",
            dataIndex: "canonicalSlug",
            width: 280,
            render: (v) => v || "-",
          },
          { title: "服务商", dataIndex: "providerName", width: 130 },
          {
            title: "排序",
            width: 130,
            render: (_, r) => {
              const index = filteredItems.findIndex((item) => item.id === r.id);
              return (
                <Space size={2}>
                  <span>{r.sortOrder}</span>
                  <Button
                    type="text"
                    size="small"
                    icon={<ArrowUpOutlined />}
                    disabled={!can("ai:write") || index <= 0 || movingId !== undefined}
                    loading={movingId === r.id}
                    aria-label="模型上移"
                    onClick={() => void move(r.id, filteredItems[index - 1].id)}
                  />
                  <Button
                    type="text"
                    size="small"
                    icon={<ArrowDownOutlined />}
                    disabled={!can("ai:write") || index < 0 || index >= filteredItems.length - 1 || movingId !== undefined}
                    aria-label="模型下移"
                    onClick={() => void move(r.id, filteredItems[index + 1].id)}
                  />
                </Space>
              );
            },
          },
          {
            title: "上架时间",
            dataIndex: "remoteCreatedAt",
            width: 120,
            render: (v) => (v ? new Date(`${v}Z`).toLocaleDateString() : "-"),
          },
          {
            title: "知识截止",
            dataIndex: "knowledgeCutoff",
            width: 120,
            render: (v) => v || "-",
          },
          { title: "参考标签", width: 240, render: (_, r) => lifecycleTags(r) },
          {
            title: "类型",
            width: 80,
            render: (_, r) =>
              isFree(r) ? <Tag color="green">免费</Tag> : <Tag>付费</Tag>,
          },
          {
            title: "状态",
            width: 80,
            render: (_, r) => (
              <Switch
                size="small"
                checked={r.enabled}
                disabled={!can("ai:write")}
                onChange={(checked) => void toggle(r, checked)}
              />
            ),
          },
          {
            title: "操作",
            fixed: "right",
            width: 120,
            render: (_, r) => (
              <Space>
                <Button type="link" disabled={!can("ai:write")} onClick={() => edit(r)}>
                  编辑
                </Button>
                <Popconfirm
                  title="确认删除？"
                  onConfirm={() => api.delete(`/models/${r.id}`).then(load)}
                >
                  <Button type="link" danger disabled={!can("ai:write")}>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? "编辑模型" : "新增模型"}
        open={open}
        onOk={save}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="providerId"
            label="服务商"
            rules={[{ required: true }]}
          >
            <Select
              showSearch
              optionFilterProp="label"
              options={providers.map((p) => ({ value: p.id, label: p.name }))}
            />
          </Form.Item>
          <Form.Item
            name="code"
            label="厂商模型编码"
            rules={[{ required: true }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="displayName"
            label="显示名称"
            rules={[{ required: true }]}
          >
            <Input />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="inputPricePerMillion" label="输入/百万 Token">
                <InputNumber min={0} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="outputPricePerMillion" label="输出/百万 Token">
                <InputNumber min={0} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="enabled" label="启用" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="sortOrder" label="排序值" help="数值越小越靠前">
                <InputNumber precision={0} style={{ width: "100%" }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="free" label="免费模型" valuePropName="checked">
                <Switch checkedChildren="免费" unCheckedChildren="付费" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </>
  );
}

function Playground({
  sidebarExpanded,
  onToggleSidebar,
}: {
  sidebarExpanded: boolean;
  onToggleSidebar: () => void;
}) {
  type ChatMessage = {
    role: "user" | "assistant";
    content: string;
    error?: boolean;
  };
  type ConversationSummary = {
    id: number;
    modelId: number;
    title: string;
    createdAt: string;
    updatedAt: string;
  };
  type MessageMarker = {
    index: number;
    top: number;
    height: number;
  };
  const { message } = AntApp.useApp();
  const [models, setModels] = useState<Model[]>([]);
  const [modelId, setModelId] = useState<number>();
  const [conversationId, setConversationId] = useState<number>();
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [prompt, setPrompt] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [messageMarkers, setMessageMarkers] = useState<MessageMarker[]>([]);
  const [activeMessageIndex, setActiveMessageIndex] = useState(0);
  const [loading, setLoading] = useState(false);
  const historyRef = useRef<HTMLDivElement>(null);
  const messageRefs = useRef<Array<HTMLDivElement | null>>([]);
  useEffect(() => {
    api.get("/models").then((r) => {
      setModels(r.data);
      setModelId(r.data.find((x: Model) => x.enabled)?.id);
    });
  }, []);
  useEffect(() => {
    let active = true;
    setMessages([]);
    setConversations([]);
    setConversationId(undefined);
    setPrompt("");
    if (modelId)
      api
        .get("/conversations", { params: { modelId } })
        .then(async (r) => {
          if (!active) return;
          const items = r.data as ConversationSummary[];
          setConversations(items);
          if (!items[0]) return;
          const history = await api.get(
            `/conversations/${items[0].id}/messages`,
          );
          if (active) {
            setConversationId(items[0].id);
            setMessages(
              history.data.map((item: ChatMessage) => ({
                role: item.role,
                content: item.content,
              })),
            );
          }
        });
    return () => {
      active = false;
    };
  }, [modelId]);
  useEffect(() => {
    const frame = requestAnimationFrame(() => {
      const history = historyRef.current;
      if (!history || messages.length === 0) return;
      const latestIndex = messages.length - 1;
      const latestMessage = messages[latestIndex];
      const latestElement = messageRefs.current[latestIndex];
      if (latestMessage.role === "assistant" && latestElement) {
        history.scrollTo({
          top: Math.max(0, latestElement.offsetTop - 16),
          behavior: "smooth",
        });
        return;
      }
      history.scrollTo({ top: history.scrollHeight, behavior: "smooth" });
    });
    return () => cancelAnimationFrame(frame);
  }, [messages]);
  useEffect(() => {
    const history = historyRef.current;
    if (!history) return;
    messageRefs.current.length = messages.length;
    let geometryFrame = 0;
    let activeFrame = 0;
    const updateActiveMessage = () => {
      cancelAnimationFrame(activeFrame);
      activeFrame = requestAnimationFrame(() => {
        const viewportCenter = history.scrollTop + history.clientHeight / 2;
        let closestIndex = 0;
        let closestDistance = Number.POSITIVE_INFINITY;
        messageRefs.current.forEach((element, index) => {
          if (!element) return;
          const center = element.offsetTop + element.offsetHeight / 2;
          const distance = Math.abs(center - viewportCenter);
          if (distance < closestDistance) {
            closestDistance = distance;
            closestIndex = index;
          }
        });
        setActiveMessageIndex(closestIndex);
      });
    };
    const updateMarkerGeometry = () => {
      cancelAnimationFrame(geometryFrame);
      geometryFrame = requestAnimationFrame(() => {
        const contentHeight = Math.max(history.scrollHeight, 1);
        setMessageMarkers(
          messageRefs.current.flatMap((element, index) =>
            element
              ? [
                  {
                    index,
                    top: Math.min(98, (element.offsetTop / contentHeight) * 100),
                    height: Math.max(
                      3,
                      Math.min(12, (element.offsetHeight / contentHeight) * 100),
                    ),
                  },
                ]
              : [],
          ),
        );
        updateActiveMessage();
      });
    };
    const resizeObserver = new ResizeObserver(updateMarkerGeometry);
    resizeObserver.observe(history);
    messageRefs.current.forEach((element) => {
      if (element) resizeObserver.observe(element);
    });
    history.addEventListener("scroll", updateActiveMessage, { passive: true });
    updateMarkerGeometry();
    return () => {
      history.removeEventListener("scroll", updateActiveMessage);
      resizeObserver.disconnect();
      cancelAnimationFrame(geometryFrame);
      cancelAnimationFrame(activeFrame);
    };
  }, [messages, loading]);
  const scrollToMessage = (index: number) => {
    const history = historyRef.current;
    const target = messageRefs.current[index];
    if (!history || !target) return;
    history.scrollTo({
      top: Math.max(0, target.offsetTop - history.clientHeight / 3),
      behavior: "smooth",
    });
  };
  const openConversation = async (id: number) => {
    if (loading || id === conversationId) return;
    try {
      const history = await api.get(`/conversations/${id}/messages`);
      setConversationId(id);
      setMessages(
        history.data.map((item: ChatMessage) => ({
          role: item.role,
          content: item.content,
        })),
      );
      setPrompt("");
    } catch (e: any) {
      message.error(e.response?.data?.message || e.message || "加载对话失败");
    }
  };
  const newConversation = async () => {
    if (!modelId || loading) return;
    try {
      const r = await api.post("/conversations", { modelId });
      setConversationId(r.data.id);
      setConversations((current) => [
        { ...r.data, title: "新对话" },
        ...current,
      ]);
      setMessages([]);
      setPrompt("");
    } catch (e: any) {
      message.error(e.response?.data?.message || e.message || "新建对话失败");
    }
  };
  const copy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      message.success("已复制");
    } catch {
      message.error("复制失败");
    }
  };
  const submitMessage = async (rawContent: string, clearComposer = false) => {
    const content = rawContent.trim();
    if (!modelId || !content || loading) return;
    const userMessage: ChatMessage = { role: "user", content };
    const requestContext = [
      ...messages.filter((item) => !item.error),
      userMessage,
    ];
    setMessages((current) => [...current, userMessage]);
    if (conversationId) {
      setConversations((current) =>
        current.map((item) =>
          item.id === conversationId
            ? {
                ...item,
                title: item.title === "新对话" ? content.slice(0, 60) : item.title,
                updatedAt: new Date().toISOString(),
              }
            : item,
        ),
      );
    }
    if (clearComposer) setPrompt("");
    setLoading(true);
    try {
      const r = await api.post("/chat/completions", {
        modelId,
        messages: requestContext,
        conversationId,
      });
      setConversationId(r.data.conversationId);
      const responseContent =
        typeof r.data.content === "string" ? r.data.content.trim() : "";
      if (!responseContent) throw new Error("大模型返回了空内容");
      setMessages((current) => [
        ...current,
        { role: "assistant", content: responseContent },
      ]);
    } catch (e: any) {
      const reason =
        e.response?.data?.message || e.message || "大模型未返回有效响应";
      const errorText = `请求失败：${String(reason)}`;
      setMessages((current) => [
        ...current,
        { role: "assistant", content: errorText, error: true },
      ]);
    } finally {
      setLoading(false);
    }
  };
  const send = async () => submitMessage(prompt, true);
  const resend = async (content: string) => submitMessage(content);
  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      void send();
    }
  };
  return (
    <Card className="chat-card">
      <div className="chat-layout">
        <div className="chat-toolbar">
          <Tooltip title={sidebarExpanded ? "折叠菜单" : "展开菜单"}>
            <Button
              className="chat-tool-button"
              type="text"
              shape="circle"
              icon={<MenuOutlined />}
              aria-label={sidebarExpanded ? "折叠菜单" : "展开菜单"}
              aria-expanded={sidebarExpanded}
              onClick={onToggleSidebar}
            />
          </Tooltip>
          <Space size={4}>
            <Tooltip title="切换模型" mouseEnterDelay={0.5}>
              <Select
                className="model-picker"
                popupClassName="model-picker-popup"
                value={modelId}
                onChange={setModelId}
                showSearch
                variant="borderless"
                filterOption={(input, option) => {
                  const model = models.find((item) => item.id === option?.value);
                  return model
                    ? `${model.displayName} ${model.code} ${model.providerName}`
                        .toLocaleLowerCase()
                        .includes(input.toLocaleLowerCase())
                    : false;
                }}
                options={models
                  .filter((m) => m.enabled)
                  .map((m) => ({
                    value: m.id,
                    label: m.displayName,
                  }))}
                dropdownRender={(menu) => (
                  <div>
                    <div className="model-picker-title">模型</div>
                    {menu}
                  </div>
                )}
                optionRender={(option) => {
                  const model = models.find((item) => item.id === option.value);
                  return (
                    <div className="model-picker-option">
                      <div className="model-picker-option-name">
                        {model?.displayName ?? option.label}
                      </div>
                      <div className="model-picker-option-description">
                        {model ? `${model.providerName} · ${model.code}` : ""}
                      </div>
                    </div>
                  );
                }}
                menuItemSelectedIcon={<CheckOutlined />}
                placeholder="选择模型"
                style={{ minWidth: 280 }}
              />
            </Tooltip>
            <Tooltip title="历史对话">
              <Button
                className="chat-tool-button"
                type="text"
                shape="circle"
                icon={<ConversationHistoryIcon />}
                aria-label="打开历史对话"
                onClick={() => setHistoryOpen(true)}
              />
            </Tooltip>
            <Tooltip title="新建对话">
              <Button
                className="chat-tool-button"
                type="text"
                shape="circle"
                icon={<NewConversationIcon />}
                disabled={!modelId || loading}
                aria-label="新建对话"
                onClick={newConversation}
              />
            </Tooltip>
          </Space>
        </div>
        <Drawer
          title="历史对话"
          placement="left"
          width={320}
          open={historyOpen}
          onClose={() => setHistoryOpen(false)}
          styles={{ body: { padding: 8 } }}
        >
          <div className="conversation-list-items conversation-drawer-list">
            {conversations.length === 0 && (
              <div className="conversation-list-empty">暂无历史对话</div>
            )}
            {conversations.map((item) => (
              <button
                type="button"
                className={`conversation-item${item.id === conversationId ? " active" : ""}`}
                key={item.id}
                title={item.title}
                disabled={loading}
                onClick={() => {
                  void openConversation(item.id);
                  setHistoryOpen(false);
                }}
              >
                <span className="conversation-item-title">{item.title}</span>
                <span className="conversation-item-time">
                  {new Date(item.updatedAt).toLocaleString("zh-CN", {
                    month: "2-digit",
                    day: "2-digit",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </span>
              </button>
            ))}
          </div>
        </Drawer>
        <div className="chat-workspace">
          <div
            className={`chat-main${messages.length === 0 && !loading ? " chat-main-empty" : ""}`}
          >
            <div className="chat-history-shell">
            <div className="chat-history" ref={historyRef}>
              {messages.length === 0 && (
                <div className="chat-empty">开始一段新对话</div>
              )}
              {messages.map((item, index) => (
                <div
                  className={`chat-row ${item.role}${item.error ? " chat-row-error" : ""}`}
                  key={index}
                  data-message-index={index}
                  ref={(element) => {
                    messageRefs.current[index] = element;
                  }}
                >
                  <div className="chat-role">
                    {item.role === "user" ? "你" : "AI"}
                  </div>
                  <div className="chat-message-body">
                    <div className="chat-bubble">
                      <MessageContent content={item.content} onCopy={copy} />
                    </div>
                    <div className="message-actions">
                      {item.role === "user" && (
                        <Tooltip title="重发">
                          <Button
                            className="message-action-button"
                            type="text"
                            shape="circle"
                            size="small"
                            icon={<ResendIcon />}
                            disabled={loading}
                            aria-label="重发消息"
                            onClick={() => void resend(item.content)}
                          />
                        </Tooltip>
                      )}
                      <Tooltip title="复制">
                        <Button
                          className="message-action-button"
                          type="text"
                          shape="circle"
                          size="small"
                          icon={<CopyIcon />}
                          aria-label="复制消息"
                          onClick={() => copy(item.content)}
                        />
                      </Tooltip>
                    </div>
                  </div>
                </div>
              ))}
              {loading && (
                <div className="chat-row assistant">
                  <div className="chat-role">AI</div>
                  <div className="chat-bubble chat-thinking">正在思考…</div>
                </div>
              )}
            </div>
            {messages.length > 1 && (
              <nav className="chat-minimap" aria-label="对话导航">
                {messageMarkers.map((marker) => {
                  const item = messages[marker.index];
                  if (!item) return null;
                  const preview = item.content.replace(/\s+/g, " ").slice(0, 90);
                  return (
                    <Tooltip
                      key={marker.index}
                      placement="left"
                      title={
                        <div className="minimap-preview">
                          <strong>{item.role === "user" ? "你" : "AI"}</strong>
                          <div>{preview}{item.content.length > 90 ? "…" : ""}</div>
                        </div>
                      }
                    >
                      <button
                        type="button"
                        className={`minimap-marker ${item.role}${marker.index === activeMessageIndex ? " active" : ""}`}
                        style={{ top: `${marker.top}%`, height: `${marker.height}px` }}
                        aria-label={`跳转到第 ${marker.index + 1} 条消息`}
                        onClick={() => scrollToMessage(marker.index)}
                      />
                    </Tooltip>
                  );
                })}
              </nav>
            )}
            </div>
            <div className="chat-composer">
              <Input.TextArea
                rows={3}
                variant="borderless"
                value={prompt}
                disabled={!can("ai:execute")}
                onChange={(e) => setPrompt(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="输入消息，Enter 发送，Shift+Enter 换行"
              />
              <Tooltip title="发送">
                <Button
                  className="chat-send"
                  type="primary"
                  shape="circle"
                  icon={<SendOutlined />}
                  loading={loading}
                  disabled={!can("ai:execute") || !modelId || !prompt.trim()}
                  aria-label="发送消息"
                  onClick={send}
                />
              </Tooltip>
            </div>
          </div>
        </div>
      </div>
    </Card>
  );
}

const consolePages = ["dashboard", "providers", "models", "playground"] as const;
type ConsolePage = (typeof consolePages)[number];

function pageFromHash(): ConsolePage {
  const page = window.location.hash.slice(1);
  return consolePages.includes(page as ConsolePage)
    ? (page as ConsolePage)
    : "dashboard";
}

function ConsoleApp() {
  const [page, setPage] = useState<ConsolePage>(pageFromHash);
  const [sidebarExpanded, setSidebarExpanded] = useState(false);
  const [sidebarHovered, setSidebarHovered] = useState(false);
  const sidebarVisible = sidebarExpanded || sidebarHovered;
  useEffect(() => {
    const syncPageFromHash = () => setPage(pageFromHash());
    window.addEventListener("hashchange", syncPageFromHash);
    return () => window.removeEventListener("hashchange", syncPageFromHash);
  }, []);
  const navigatePage = (nextPage: string) => {
    if (!consolePages.includes(nextPage as ConsolePage)) return;
    const validPage = nextPage as ConsolePage;
    setPage(validPage);
    if (window.location.hash !== `#${validPage}`) {
      window.location.hash = validPage;
    }
  };
  const toggleSidebar = () => {
    const nextExpanded = !sidebarExpanded;
    setSidebarExpanded(nextExpanded);
    if (!nextExpanded) setSidebarHovered(false);
  };
  const pages: Record<ConsolePage, ReactNode> = {
    dashboard: <Dashboard />,
    providers: <Providers />,
    models: <Models />,
    playground: (
      <Playground
        sidebarExpanded={sidebarExpanded}
        onToggleSidebar={toggleSidebar}
      />
    ),
  };
  return (
    <AntApp>
      <Layout className="shell">
        {!sidebarExpanded && (
          <div
            className="sidebar-hover-trigger"
            aria-hidden="true"
            onMouseEnter={() => setSidebarHovered(true)}
          />
        )}
        <Sider
          width={230}
          collapsedWidth={sidebarVisible ? 72 : 0}
          collapsed={!sidebarExpanded}
          trigger={null}
          theme="light"
          onMouseEnter={() => setSidebarHovered(true)}
          onMouseLeave={() => {
            if (!sidebarExpanded) setSidebarHovered(false);
          }}
        >
          <div
            className={`brand ${!sidebarExpanded ? "brand-collapsed" : ""}`}
          >
            <RobotOutlined />
            {sidebarExpanded && <span>AI Console</span>}
          </div>
          <Menu
            mode="inline"
            selectedKeys={[page]}
            onClick={(e) => navigatePage(e.key)}
            items={[
              {
                key: "dashboard",
                icon: <BarChartOutlined />,
                label: "数据概览",
              },
              { key: "providers", icon: <ApiOutlined />, label: "AI 服务商" },
              { key: "models", icon: <DatabaseOutlined />, label: "模型管理" },
              {
                key: "playground",
                icon: <MessageOutlined />,
                label: "调用测试",
              },
            ]}
          />
          <div
            className="sidebar-bottom"
            role="button"
            tabIndex={0}
            aria-label={sidebarExpanded ? "折叠菜单" : "展开菜单"}
            aria-expanded={sidebarExpanded}
            onClick={toggleSidebar}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                toggleSidebar();
              }
            }}
          >
            {sidebarExpanded ? <MenuFoldOutlined /> : <MenuUnfoldOutlined />}
          </div>
        </Sider>
        <Layout>
          {page !== "playground" && (
            <Header className="header">
              <Tooltip title={sidebarExpanded ? "折叠菜单" : "展开菜单"}>
                <Button
                  className="sidebar-header-toggle"
                  type="text"
                  shape="circle"
                  icon={<MenuOutlined />}
                  aria-label={sidebarExpanded ? "折叠菜单" : "展开菜单"}
                  aria-expanded={sidebarExpanded}
                  onClick={toggleSidebar}
                />
              </Tooltip>
            </Header>
          )}
          <Content
            className={`content ${page === "playground" ? "content-playground" : ""}`}
          >
            {pages[page]}
          </Content>
        </Layout>
      </Layout>
    </AntApp>
  );
}

function LoginPanel({ onLogin }: { onLogin: (session: Session) => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const submit = async (values: { username: string; password: string }) => {
    setBusy(true);
    setError("");
    try {
      const session = await login(values.username, values.password);
      saveSession(session);
      onLogin(session);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };
  return <Layout className="ai-login-shell"><Card className="ai-login-card"><Typography.Title level={2}>AI Console</Typography.Title><Typography.Text type="secondary">使用平台账号登录</Typography.Text>{error && <Typography.Paragraph type="danger">{error}</Typography.Paragraph>}<Form layout="vertical" onFinish={submit} style={{ marginTop: 24 }}><Form.Item name="username" label="用户名" rules={[{ required: true }]}><Input /></Form.Item><Form.Item name="password" label="密码" rules={[{ required: true }]}><Input.Password /></Form.Item><Button block type="primary" htmlType="submit" loading={busy}>登录</Button></Form></Card></Layout>;
}

export default function App() {
  const [user, setUser] = useState<UserSession | undefined>(storedUser());
  const [checking, setChecking] = useState(true);
  useEffect(() => {
    const expired = () => setUser(undefined);
    window.addEventListener("platform-auth-expired", expired);
    if (!user) me().then(value => { saveUser(value); setUser(value); }).catch(() => {}).finally(() => setChecking(false));
    else setChecking(false);
    return () => window.removeEventListener("platform-auth-expired", expired);
  }, []);
  if (checking) return <Layout className="ai-login-shell"><Typography.Text>正在检查登录状态…</Typography.Text></Layout>;
  if (!user) return <LoginPanel onLogin={session => setUser(session.user)} />;
  return <div className="ai-authenticated"><ConsoleApp /></div>;
}
