package com.platform.task.platform.service;

import com.platform.task.platform.domain.MerchantProfile;
import com.platform.task.platform.repository.MerchantProfileRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TaskCompatibilityCatalog {
    private static final List<Spec> SPECS = List.of(
            spec("dji-merchant-basicdata-modify", "大疆商户基本信息修改", "DJI", "com.platform.task.controller.dji.V2MerchantBasicdataModifyTask"),
            spec("dji-merchant-busi-open", "大疆商户业务开通", "DJI", "com.platform.task.controller.dji.V2MerchantBusiOpenTask"),
            spec("dji-merchant-busi-config", "大疆微信商户配置", "DJI", "com.platform.task.controller.dji.V2MerchantBusiConfigTask"),
            spec("dji-merchant-status-query", "大疆申请状态查询", "DJI", "com.platform.task.controller.dji.V2MerchantBasicdataStatusQueryTask"),
            spec("dji-supplementary-picture", "大疆补充图片", "DJI", "com.platform.task.controller.dji.V2SupplementaryPictureTask"),
            spec("dji-user-basicdata-query", "大疆用户基本信息查询", "DJI", "com.platform.task.controller.dji.V2UserBasicdataQueryTask"),
            spec("cotti-refund", "Cotti 扫码退款", "COTTI", "com.platform.task.controller.cotti.V2TradePaymentScanpayRefundRequestDemo"),
            spec("cotti-refund-query", "Cotti 退款查询", "COTTI", "com.platform.task.controller.cotti.V2TradePaymentScanpayRefundqueryTask"),
            spec("hema-merchant-basicdata-modify", "盒马商户基本信息修改", "HEMA", "com.platform.task.controller.hema.V2MerchantBasicdataModifyTask"),
            spec("yuanzu-merchant-basicdata-modify", "元祖商户基本信息修改", "YUANZU", "com.platform.task.controller.yuanzu.V2MerchantBasicdataModifyTask"),
            spec("yuanzu-merchant-status-query", "元祖申请状态查询", "YUANZU", "com.platform.task.controller.yuanzu.V2MerchantBasicdataStatusQueryTask"),
            spec("yuanzu-supplementary-picture", "元祖补充图片", "YUANZU", "com.platform.task.controller.yuanzu.V2SupplementaryPictureTask"),
            spec("hezhao-merchant-basicdata-modify", "和兆商户基本信息修改", "HEZHAO", "com.platform.task.controller.hezhao.V2MerchantBasicdataModifyTask"),
            spec("hezhao-supplementary-picture", "和兆补充图片", "HEZHAO", "com.platform.task.controller.hezhao.V2SupplementaryPictureTask"),
            spec("quhulian-merchant-basicdata-modify", "趣互联商户基本信息修改", "QUHULIAN", "com.platform.task.controller.quhulian.V2MerchantBasicdataModifyTask"),
            spec("quhulian-merchant-mcc-modify", "趣互联商户 MCC 修改", "QUHULIAN", "com.platform.task.controller.quhulian.ModifyMerchantMccPostTask"),
            spec("huifu-merchant-busi-open", "汇付商户业务开通", "HUIFU", "com.platform.task.controller.huifu.V2MerchantBusiOpenTask"),
            spec("huifu-js-pay", "汇付 JS 支付", "HUIFU", "com.platform.task.controller.huifu.V2TradePaymentJspayTask"),
            spec("buluke-refund", "布鲁可扫码退款", "BULUKE", "com.platform.task.controller.buluke.V2TradePaymentScanpayRefundTask")
    );
    private static final Set<String> TEMPLATE_CLASSES = Set.of(
            "com.platform.task.controller.onboarding.EnterpriseMerchantOnboardingTask",
            "com.platform.task.controller.onboarding.IndividualMerchantOnboardingTask",
            "com.platform.task.controller.onboarding.MerchantBusinessOpenTask",
            "com.platform.task.controller.onboarding.MerchantPictureUploadTask",
            "com.platform.task.controller.onboarding.MerchantApplicationStatusQueryTask",
            "com.platform.task.controller.onboarding.MerchantBasicdataModifyTask",
            "com.platform.task.controller.onboarding.MerchantBusinessModifyTask"
    );
    private static final Map<String, String> MERCHANT_NAMES = Map.of(
            "BULUKE", "布鲁克",
            "COTTI", "库迪",
            "DJI", "大疆",
            "HEMA", "盒马",
            "HEZHAO", "和兆",
            "HUIFU", "汇付",
            "QUHULIAN", "趣互联",
            "YUANZU", "元祖"
    );
    private final Map<String, Spec> byClass = SPECS.stream().collect(Collectors.toUnmodifiableMap(Spec::className, Function.identity()));
    private final MerchantProfileRepository merchants;

    public TaskCompatibilityCatalog(MerchantProfileRepository merchants) {
        this.merchants = merchants;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void synchronize() {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, String> merchantSpec : MERCHANT_NAMES.entrySet()) {
            String code = merchantSpec.getKey();
            MerchantProfile merchant = merchants.findByCodeIgnoreCase(code).orElseGet(() -> {
                MerchantProfile value = new MerchantProfile();
                value.setId(code.toLowerCase());
                value.setCode(code);
                value.setDescription("由内置任务模板自动注册");
                value.setConfigurationJson("{}");
                value.setCreatedAt(now);
                return value;
            });
            merchant.setName(merchantSpec.getValue());
            merchant.setUpdatedAt(now);
            merchants.save(merchant);
        }
    }

    public Method requireMain(String className) {
        if (!byClass.containsKey(className) && !TEMPLATE_CLASSES.contains(className)) {
            throw new IllegalStateException("Task class is not in the compatibility whitelist");
        }
        try {
            Method method = Class.forName(className).getMethod("main", String[].class);
            if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())) {
                throw new IllegalStateException("Task main method must be public static");
            }
            return method;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot load task entry point: " + className, e);
        }
    }

    public int size() { return SPECS.size(); }
    private static Spec spec(String id, String name, String category, String className) { return new Spec(id, name, category, className); }
    private record Spec(String id, String displayName, String category, String className) {}
}
