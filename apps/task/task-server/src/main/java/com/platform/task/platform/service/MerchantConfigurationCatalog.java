package com.platform.task.platform.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MerchantConfigurationCatalog {
    record Field(String key, String label, String description, String placeholder,
                 boolean required, boolean secret, boolean multiline) {}

    private static final List<Field> FIELDS = List.of(
            new Field("sys_id", "系统号", "汇付分配的系统标识。", "例如：6666000109133323", true, false, false),
            new Field("huifu_id", "汇付商户号", "当前商户在汇付体系中的商户号。", "请输入汇付商户号", true, false, false),
            new Field("product_id", "产品号", "汇付分配的产品标识。", "例如：YYZY", true, false, false),
            new Field("upper_huifu_id", "上级汇付商户号", "存在上级商户或渠道关系时填写，没有可留空。", "选填", false, false, false),
            new Field("huifu_public_key", "汇付公钥", "用于验证汇付返回数据，请粘贴完整公钥内容。", "粘贴汇付公钥", true, false, true),
            new Field("merchant_public_key", "商户公钥", "商户自身的公钥，请粘贴完整公钥内容。", "粘贴商户公钥", true, false, true),
            new Field("merchant_private_key", "商户私钥", "用于商户请求签名，仅授权用户可查看和修改。", "粘贴商户私钥", true, true, true)
    );
    private static final Set<String> KEYS = FIELDS.stream().map(Field::key).collect(java.util.stream.Collectors.toUnmodifiableSet());

    private MerchantConfigurationCatalog() {}

    static List<Field> fields() { return FIELDS; }

    static Map<String, String> normalize(Map<String, String> configuration) {
        Map<String, String> supplied = configuration == null ? Map.of() : configuration;
        for (String key : supplied.keySet()) {
            if (key == null || !KEYS.contains(key)) {
                throw new IllegalArgumentException("Unknown merchant configuration field: " + key);
            }
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Field field : FIELDS) {
            String value = supplied.get(field.key());
            if (value != null && !value.isBlank()) normalized.put(field.key(), value.trim());
        }
        for (Field field : FIELDS) {
            if (field.required() && !normalized.containsKey(field.key())) {
                throw new IllegalArgumentException("Missing merchant configuration: " + field.label());
            }
        }
        return normalized;
    }
}
