package com.platform.task.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.task.platform.web.TaskDtos.InputFieldResponse;
import com.platform.task.platform.web.TaskDtos.OptionResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Structured page fields backed only by argument names and paths present in the legacy tasks. */
@Component
public class TaskInputCatalog {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<OptionResponse> RUN_MODES = List.of(
            new OptionResponse("仅第一条", "first"),
            new OptionResponse("全部", "batch"),
            new OptionResponse("跳过第一条", "remaining")
    );
    private static final String HUIFU_UPLOAD_FILE_TYPES = "F01,F02,F03,F04,F05,F06,F07,F08,F09,F10,F11,F12,F13,F14,F15,F16,F17,F18,F19,F20,F21,F22,F23,F24,F25,F26,F27,F28,F29,F30,F31,F32,F33,F34,F35,F36,F37,F38,F39,F40,F41,F42,F43,F44,F45,F46,F47,F48,F49,F50,F51,F52,F53,F54,F55,F56,F57,F58,F60,F61,F62,F63,F64,F65,F66,F67,F68,F69,F70,F71,F72,F73,F74,F75,F76,F77,F78,F79,F80,F81,F82,F83,F84,F85,F86,F87,F88,F89,F90,F91,F95,F96,F97,F98,F99,F100,F101,F102,F103,F104,F105,F106,F107,F108,F109,F110,F111,F112,F113,F114,F115,F116,F117,F118,F119,F120,F121,F150,F151,F152,F153,F154,F155,F224,F227,F228,F229,F230,F231,F232,F233,F234,F235,F236,F237,F238,F239,F240,F241,F242,F243,F244,F245,F246,F247,F301,F302,F303,F304,F305,F306,F307,F308,F309,F310,F311,F312,F313,F314,F315,F316,F317,F318,F319,F320,F321,F322,F323,F324,F325,F326,F327,F328,F329,F330,F331,F332,F333,F334,F335,F336,F337,F338,F339,F340,F341,F342,F343,F344,F345,F346,F347,F348,F349,F350,F351,F352,F353,F354,F355,F356,F357,F358,F359,F360,F361,F362,F363,F364,F365,F366,F367,F368,F369,F370,F371,F372,F373,F374,F375,F376,F377,F378,F379,F380,F381,F382,F383,F384,F385,F386,F387,F388,F389,F390,F391,F392,F393,F394,F395,F396,F397,F398,F399,F400,F401,F402,F403,F404,F405,F406,F407,F408,F409,F410,F411,F412,F413,F414,F415,F416,F417,F418,F419,F420,F421,F422,F423,F424,F425,F426,F427,F428,F429,F430,F431,F432,F433,F434,F435,F436,F437,F438,F439,F440,F441,F442,F443,F444,F445,F446,F447,F448,F449,F450,F451,F452,F453,F454,F455,F456,F457,F458,F459,F460,F461,F462,F463,F464,F465,F466,F467,F468,F469,F470,F471,F472,F473,F474,F475,F476,F477,F478,F479,F480,F481,F482,F483,F484,F485,F486,F487,F488,F489,F490,F491,F492,F493,F494,F495,F496,F497,F503,F504,F505,F506,F511,F512,F513,F514,F515,F516,F518,F519,F520,F521,F522,F523,F524,F528,F529,F531,F532,F533,F534,F535,F536,F537,F538,F539,F540,F541,F542,F543,F544,F550,F551,F552,F553,F554,F555,F556,F557,F558,F559,F560,F561,F562,F563,F564,F565,F566,F567,F568,F569,F570,F571,F572,F573,F574,F575,F576,F577,F578,F579,F580,F581,F583,F590,F591,F592,F593,F605,F606,F607,F608,F610,F611,F612,F617,F620,F621,F622,F623,F624,F633,F634,F635,F636,F637,F652,F653,F654,F655,F672,F673,F674,F675,F676,F677,F678,F679,F680,F681,F682,F683,F684,F685,F686,F687,F718,F719,F720,F721,F722,F723,F724,F725,F726,F727";
    private static final Map<String, String> COMMON_FILE_TYPE_NAMES = Map.ofEntries(
            Map.entry("F01", "法人身份证正反面"), Map.entry("F02", "法人身份证人像面"),
            Map.entry("F03", "法人身份证国徽面"), Map.entry("F07", "营业执照"),
            Map.entry("F08", "开户许可证"), Map.entry("F13", "结算银行卡"),
            Map.entry("F22", "（线下场景）门头照"), Map.entry("F24", "（线下场景）店铺内景照"),
            Map.entry("F55", "结算人身份证国徽面"), Map.entry("F56", "结算人身份证人像面"),
            Map.entry("F105", "（线下场景）收银台"), Map.entry("F117", "其他"),
            Map.entry("F244", "签约人身份证照片-人像面"), Map.entry("F247", "签约人身份证照片-国徽面"),
            Map.entry("F368", "门头照"), Map.entry("F550", "银行卡正面"));
    private static final List<OptionResponse> HUIFU_FILE_TYPES = java.util.Arrays.stream(HUIFU_UPLOAD_FILE_TYPES.split(","))
            .map(code -> new OptionResponse(COMMON_FILE_TYPE_NAMES.getOrDefault(code, code) + "（" + code + "）", code))
            .toList();

    private final Map<String, FormSpec> forms = Map.ofEntries(
            entry("dji-merchant-basicdata-modify", context(path("inputPath", "输入 Excel 路径", true,
                    "Excel 的 D 列为 huifu_id，F 列为 ext_mer_id"))),
            entry("dji-merchant-busi-open", lines("items", area("items", "汇付商户号", true, "每行一个 huifu_id"))),
            entry("dji-merchant-busi-config", lines("items", area("items", "汇付商户号", true, "每行一个 huifu_id"))),
            entry("dji-merchant-status-query", context(path("outputDir", "结果目录", false,
                    "扫描其中含 apply_no 的任务结果 Excel；不填时使用 output"))),
            entry("dji-supplementary-picture", lines("items", area("items", "图片路径", true, "每行一个待上传文件路径"))),
            entry("dji-user-basicdata-query", lines("items", area("items", "汇付商户号", true, "每行一个 huifu_id"))),
            entry("cotti-refund", context(path("inputPath", "退款 Excel 路径", true,
                    "表头沿用原任务中的 pay_hf_seq_id、pay_req_date、huifu_id、ord_amt 等字段"))),
            entry("cotti-refund-query", context(path("inputPath", "退款 Excel 路径", true,
                    "读取原退款任务写回的 refund_req_seq_id/refund_req_date"))),
            entry("hema-merchant-basicdata-modify", context(
                    path("inputPath", "商户 Excel 路径", true, "D 列 huifu_id、F 列 ext_mer_id"),
                    path("auxInputPath", "结算卡 Excel 路径", true, "A-D/H-J 列沿用原任务映射"))),
            entry("yuanzu-merchant-basicdata-modify", lines("items", area("items", "汇付商户号", true,
                    "每行一个 huifu_id；页面输入只处理填写的商户"))),
            entry("yuanzu-merchant-status-query", lines("items", area("items", "查询项", true,
                    "每行格式：huifuId:applyNo"))),
            entry("yuanzu-supplementary-picture", context(path("inputPath", "上传文件路径", true, "原 F480 文件"))),
            entry("hezhao-merchant-basicdata-modify", modeWithContext(
                    path("inputPath", "商户 Excel 路径", true, "B 列为 huifu_id"),
                    path("outputDir", "输出目录", false, "不填时沿用原输出目录"),
                    select("mode", "执行范围", "first", RUN_MODES))),
            entry("hezhao-supplementary-picture", context(path("inputPath", "图片路径", true, "待上传图片文件"))),
            entry("quhulian-merchant-basicdata-modify", context(path("inputPath", "商户 Excel 路径", true,
                    "Sheet2 的 A 列为 huifu_id，结果写回 C/D 列"))),
            entry("quhulian-merchant-mcc-modify", named(
                    text("huifu_id", "汇付商户号", true, "6666000158718563"),
                    text("mcc_code", "MCC", true, "8299"),
                    text("login_type", "登录类型", false, "1"),
                    text("origin_system", "来源系统", false, "ssp.operator"),
                    text("pay_way", "支付方式", false, "U"),
                    text("platform_id", "平台 ID", false, "1"),
                    text("product_id", "产品 ID", false, "SPIN"),
                    text("role_type", "角色类型", false, "1"),
                    text("sys_id", "系统 ID", false, "1"),
                    text("user_id", "操作用户 ID", false, "ronggui.liao_c"),
                    text("user_name", "操作用户名", false, "廖荣贵"),
                    text("user_type", "用户类型", false, "3"),
                    password("hf_token", "hf_token", false, "不填则沿用原任务的环境变量/默认行为"))),
            entry("huifu-merchant-busi-open", named(
                    text("huifu_id", "汇付客户 ID", true, ""),
                    text("upper_huifu_id", "上级主体 ID", true, ""))),
            entry("huifu-js-pay", named(
                    text("huifu_id", "汇付商户号", true, ""),
                    text("goods_desc", "商品描述", true, "hibs自动化-通用版验证"),
                    text("trade_type", "交易类型", true, "A_NATIVE"),
                    text("trans_amt", "交易金额", true, "0.10"))),
            entry("buluke-refund", named(
                    text("huifu_id", "汇付商户号", true, "6666000189266476"),
                    text("ord_amt", "退款金额", true, "388.05"),
                    text("org_req_date", "原交易请求日期", true, "20260701"),
                    text("org_hf_seq_id", "原交易全局流水号", true, "002900HS11A260701181825P0470a4f253b00000"))),
            entry("merchant-enterprise-onboarding", api(
                    text("upper_huifu_id", "渠道商号", true, ""),
                    text("reg_name", "商户名称", true, ""),
                    text("short_name", "商户简称", false, ""),
                    text("receipt_name", "小票名称", false, ""),
                    text("ent_type", "公司类型", false, ""),
                    text("mcc", "所属行业 MCC", false, ""),
                    text("busi_type", "经营类型", false, ""),
                    text("scene_type", "场景类型", false, ""),
                    text("license_pic", "证照图片文件 ID", false, ""),
                    text("license_code", "证照编号", false, ""),
                    text("license_validity_type", "证照有效期类型", false, ""),
                    text("license_begin_date", "证照有效期开始日期", false, ""),
                    text("license_end_date", "证照有效期截止日期", false, ""),
                    text("found_date", "成立日期", false, ""),
                    text("reg_capital", "注册资本", false, ""),
                    text("reg_district_id", "注册区编码", false, ""),
                    text("reg_detail", "注册详细地址", false, ""),
                    text("district_id", "经营区编码", false, ""),
                    text("detail_addr", "经营详细地址", false, ""),
                    text("legal_name", "法人姓名", false, ""),
                    text("legal_cert_type", "法人证件类型", false, ""),
                    text("legal_cert_no", "法人证件号码", false, ""),
                    text("legal_cert_validity_type", "法人证件有效期类型", false, ""),
                    text("legal_cert_begin_date", "法人证件有效期开始日期", false, ""),
                    text("legal_cert_end_date", "法人证件有效期截止日期", false, ""),
                    text("legal_addr", "法人证件地址", false, ""),
                    text("legal_cert_back_pic", "法人身份证国徽面文件 ID", false, ""),
                    text("legal_cert_front_pic", "法人身份证人像面文件 ID", false, ""),
                    text("contact_mobile_no", "管理员手机号", false, ""),
                    text("contact_email", "管理员邮箱", false, ""),
                    text("login_name", "管理员账号", false, ""),
                    text("reg_acct_pic", "开户许可证文件 ID", false, ""),
                    json("card_info", "银行卡信息配置 JSON", false),
                    text("settle_card_front_pic", "银行卡卡号面文件 ID", false, ""),
                    text("settle_cert_back_pic", "持卡人身份证国徽面文件 ID", false, ""),
                    text("settle_cert_front_pic", "持卡人身份证人像面文件 ID", false, ""),
                    text("auth_entrust_pic", "授权委托书文件 ID", false, ""),
                    text("head_huifu_id", "上级汇付 ID", false, ""),
                    text("mer_icp", "ICP备案编号", false, ""),
                    text("store_header_pic", "店铺门头照文件 ID", false, ""),
                    text("store_indoor_pic", "店铺内景照文件 ID", false, ""),
                    text("store_cashier_desk_pic", "店铺收银台照文件 ID", false, ""))),
            entry("merchant-individual-onboarding", api(
                    text("upper_huifu_id", "直属渠道号", true, ""),
                    text("reg_name", "商户名称", true, ""),
                    text("mcc", "所属行业 MCC", false, ""),
                    text("scene_type", "场景类型", false, ""),
                    text("district_id", "经营区编码", false, ""),
                    text("detail_addr", "经营详细地址", false, ""),
                    text("legal_cert_no", "负责人证件号码", false, ""),
                    text("legal_cert_begin_date", "负责人证件有效期开始日期", false, ""),
                    text("legal_cert_end_date", "负责人证件有效期截止日期", false, ""),
                    text("legal_addr", "负责人身份证地址", false, ""),
                    text("legal_cert_back_pic", "负责人身份证国徽面文件 ID", false, ""),
                    text("legal_cert_front_pic", "负责人身份证人像面文件 ID", false, ""),
                    text("contact_mobile_no", "负责人手机号", false, ""),
                    text("contact_email", "负责人邮箱", false, ""),
                    json("card_info", "结算卡信息配置 JSON", false),
                    text("settle_card_front_pic", "银行卡卡号面文件 ID", false, ""),
                    text("mer_icp", "ICP备案编号", false, ""),
                    text("store_header_pic", "店铺门头照文件 ID", false, ""),
                    text("store_indoor_pic", "店铺内景照文件 ID", false, ""),
                    text("store_cashier_desk_pic", "店铺收银台照文件 ID", false, ""),
                    text("head_huifu_id", "上级商户汇付 ID", false, ""))),
            entry("merchant-business-open", api(
                    text("huifu_id", "汇付客户 ID", true, ""),
                    text("upper_huifu_id", "直属渠道号", true, ""),
                    json("sign_user_info", "签约人信息 JSON", false),
                    text("online_busi_type", "线上业务类型编码", false, ""),
                    json("agreement_info", "协议信息 JSON", false),
                    text("withhold_pay_scene", "代扣场景", false, ""))),
            entry("merchant-basicdata-modify", api(
                    text("huifu_id", "汇付客户 ID", true, ""),
                    text("upper_huifu_id", "直属渠道号", true, ""),
                    json("sign_user_info", "签约人信息 JSON", false))),
            entry("merchant-business-modify", api(
                    text("huifu_id", "汇付客户 ID", true, ""),
                    text("online_busi_type", "线上业务类型编码", false, ""),
                    json("sign_user_info", "签约人信息 JSON", false),
                    text("withhold_pay_scene", "代扣场景", false, ""))),
            entry("merchant-picture-upload", api(
                    field("file_path", "本地图片", "IMAGE_FILE", false, "请选择图片", "与图片 URL 二选一", "", List.of(), false),
                    field("file_url", "图片 URL", "URL", false, "https://...", "与本地图片二选一，仅允许公网 HTTP/HTTPS 地址", "", List.of(), false),
                    select("file_type", "文件类型", "", HUIFU_FILE_TYPES))),
            entry("merchant-application-status-query", api(
                    text("apply_no", "申请单号", true, ""),
                    text("huifu_id", "汇付客户 ID", false, "")))
    );

    public List<InputFieldResponse> fields(String taskId) {
        return require(taskId).fields();
    }

    public Map<String, String> supportedInputs(String taskId, Map<String, String> supplied) {
        Set<String> allowed = require(taskId).fields().stream()
                .map(InputFieldResponse::key).collect(java.util.stream.Collectors.toSet());
        Map<String, String> result = new LinkedHashMap<>();
        if (supplied != null) supplied.forEach((key, value) -> {
            if (allowed.contains(key) && value != null && !value.isBlank()) result.put(key, value);
        });
        return result;
    }

    public boolean hasAnyInput(String taskId, Map<String, String> supplied) {
        return !supportedInputs(taskId, supplied).isEmpty();
    }

    public PreparedInputs prepare(String taskId, Map<String, String> supplied, List<String> advancedArguments) {
        FormSpec spec = require(taskId);
        Map<String, String> normalized = normalize(spec, supplied);
        List<String> arguments = new ArrayList<>();
        if (!normalized.isEmpty()) arguments.addAll(spec.argumentAdapter().apply(normalized));
        if (advancedArguments != null) {
            advancedArguments.stream().filter(value -> value != null && !value.isBlank()).forEach(arguments::add);
        }
        Map<String, String> stored = new LinkedHashMap<>(normalized);
        spec.fields().stream().filter(InputFieldResponse::secret).forEach(field -> {
            if (stored.containsKey(field.key())) stored.put(field.key(), "******");
        });
        return new PreparedInputs(List.copyOf(arguments), Map.copyOf(normalized), Map.copyOf(stored));
    }

    private Map<String, String> normalize(FormSpec spec, Map<String, String> supplied) {
        Map<String, String> source = supplied == null ? Map.of() : supplied;
        Set<String> allowed = new LinkedHashSet<>();
        spec.fields().forEach(field -> allowed.add(field.key()));
        source.keySet().stream().filter(key -> !allowed.contains(key)).findFirst()
                .ifPresent(key -> { throw new IllegalArgumentException("Unsupported page input: " + key); });

        Map<String, String> values = new LinkedHashMap<>();
        for (InputFieldResponse field : spec.fields()) {
            String value = source.get(field.key());
            if ((value == null || value.isBlank()) && field.defaultValue() != null && !field.defaultValue().isBlank()) {
                value = field.defaultValue();
            }
            if (field.required() && (value == null || value.isBlank())) {
                throw new IllegalArgumentException("Missing required page input: " + field.label());
            }
            String normalizedValue = value == null ? null : value.trim();
            if (value != null && !value.isBlank() && "SELECT".equals(field.type())
                    && field.options().stream().noneMatch(option -> option.value().equals(normalizedValue))) {
                throw new IllegalArgumentException(field.label() + " contains an unsupported value: " + value);
            }
            if (value != null && !value.isBlank()) validateJsonShape(field, value);
            if (value != null && !value.isBlank()) values.put(field.key(), value.trim());
        }
        if (spec.fields().stream().anyMatch(field -> field.key().equals("file_path"))
                && spec.fields().stream().anyMatch(field -> field.key().equals("file_url"))) {
            boolean hasPath = values.containsKey("file_path"), hasUrl = values.containsKey("file_url");
            if (hasPath == hasUrl) throw new IllegalArgumentException("本地图片和图片 URL 必须且只能选择一种");
        }
        return values;
    }

    private FormSpec require(String taskId) {
        FormSpec spec = forms.get(taskId);
        if (spec == null) throw new IllegalArgumentException("No page input definition for task: " + taskId);
        return spec;
    }

    private static Map.Entry<String, FormSpec> entry(String id, FormSpec spec) { return Map.entry(id, spec); }
    private static FormSpec context(InputFieldResponse... fields) { return new FormSpec(List.of(fields), ignored -> List.of()); }
    private static FormSpec api(InputFieldResponse... fields) { return context(fields); }
    private static FormSpec lines(String key, InputFieldResponse field) {
        return new FormSpec(List.of(field), values -> values.getOrDefault(key, "").lines()
                .map(String::trim).filter(value -> !value.isBlank()).toList());
    }
    private static FormSpec named(InputFieldResponse... fields) {
        List<InputFieldResponse> list = List.of(fields);
        return new FormSpec(list, values -> list.stream().filter(field -> values.containsKey(field.key()))
                .map(field -> "--" + field.key() + "=" + values.get(field.key())).toList());
    }
    private static FormSpec modeWithContext(InputFieldResponse... fields) {
        List<InputFieldResponse> list = List.of(fields);
        return new FormSpec(list, values -> {
            String mode = values.getOrDefault("mode", "first");
            return "first".equals(mode) ? List.of() : List.of(mode);
        });
    }

    private static InputFieldResponse text(String key, String label, boolean required, String defaultValue) {
        return field(key, label, "TEXT", required, "", "", defaultValue, List.of(), false);
    }
    private static InputFieldResponse area(String key, String label, boolean required, String description) {
        return field(key, label, "TEXTAREA", required, "每行一项", description, "", List.of(), false);
    }
    private static InputFieldResponse json(String key, String label, boolean required) {
        return field(key, label, "JSON_OBJECT", required, "", "按接口文档填写 JSON 对象", "", List.of(), false);
    }
    private static InputFieldResponse jsonArray(String key, String label, boolean required) {
        return field(key, label, "JSON_ARRAY", required, "", "可增加多个 JSON Object", "", List.of(), false);
    }
    private static InputFieldResponse path(String key, String label, boolean required, String description) {
        return field(key, label, "PATH", required, "请输入任务服务器可访问的绝对路径", description, "", List.of(), false);
    }
    private static InputFieldResponse password(String key, String label, boolean required, String description) {
        return field(key, label, "PASSWORD", required, "", description, "", List.of(), true);
    }
    private static InputFieldResponse select(String key, String label, String defaultValue, List<OptionResponse> options) {
        return field(key, label, "SELECT", true, "", "", defaultValue, options, false);
    }
    private static InputFieldResponse field(String key, String label, String type, boolean required,
                                            String placeholder, String description, String defaultValue,
                                            List<OptionResponse> options, boolean secret) {
        return new InputFieldResponse(key, label, type, required, placeholder, description, defaultValue, options, secret);
    }

    private static void validateJsonShape(InputFieldResponse field, String value) {
        if (!"JSON_OBJECT".equals(field.type()) && !"JSON_ARRAY".equals(field.type())) return;
        try {
            JsonNode node = JSON.readTree(value);
            if ("JSON_OBJECT".equals(field.type()) && !node.isObject()) {
                throw new IllegalArgumentException(field.label() + " must be a JSON object");
            }
            if ("JSON_ARRAY".equals(field.type())
                    && !isArrayOfObjects(node)) {
                throw new IllegalArgumentException(field.label() + " must be an array of JSON objects");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field.label() + " contains invalid JSON", e);
        }
    }

    private static boolean isArrayOfObjects(JsonNode node) {
        if (!node.isArray()) return false;
        for (JsonNode item : node) if (!item.isObject()) return false;
        return true;
    }

    private record FormSpec(List<InputFieldResponse> fields,
                            Function<Map<String, String>, List<String>> argumentAdapter) {}
    public record PreparedInputs(List<String> arguments, Map<String, String> runtimeInputs,
                                 Map<String, String> storedInputs) {}
}
