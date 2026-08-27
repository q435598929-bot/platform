package com.platform.task.platform.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskInputCatalogTest {
    private final TaskInputCatalog catalog = new TaskInputCatalog();

    @Test
    void convertsMerchantLinesToLegacyMainArguments() {
        TaskInputCatalog.PreparedInputs prepared = catalog.prepare("dji-merchant-busi-open",
                Map.of("items", "6661\n6662"), List.of());
        assertThat(prepared.arguments()).containsExactly("6661", "6662");
    }

    @Test
    void convertsNamedFieldsAndRedactsSecretHistory() {
        TaskInputCatalog.PreparedInputs prepared = catalog.prepare("quhulian-merchant-mcc-modify",
                Map.of("huifu_id", "6661", "mcc_code", "8299", "hf_token", "secret"), List.of());
        assertThat(prepared.arguments()).contains("--huifu_id=6661", "--mcc_code=8299", "--hf_token=secret");
        assertThat(prepared.runtimeInputs()).containsEntry("hf_token", "secret");
        assertThat(prepared.storedInputs()).containsEntry("hf_token", "******");
    }

    @Test
    void exposesMerchantOnboardingFieldsWithoutLegacyArguments() {
        assertThat(catalog.fields("merchant-enterprise-onboarding"))
                .extracting(field -> field.key())
                .contains("upper_huifu_id", "reg_name", "card_info", "legal_cert_no");
        assertThat(catalog.fields("merchant-individual-onboarding"))
                .extracting(field -> field.key())
                .contains("upper_huifu_id", "reg_name", "settle_card_front_pic");
        assertThat(catalog.fields("merchant-business-open"))
                .extracting(field -> field.key())
                .containsExactly("huifu_id", "upper_huifu_id", "sign_user_info", "online_busi_type",
                        "agreement_info", "withhold_pay_scene");
        assertThat(catalog.fields("merchant-picture-upload"))
                .extracting(field -> field.key())
                .containsExactly("file_path", "file_url", "file_type");
        assertThat(catalog.fields("merchant-picture-upload"))
                .filteredOn(field -> field.key().equals("file_type"))
                .singleElement().satisfies(field -> assertThat(field.options()).hasSizeGreaterThan(400));
        assertThat(catalog.fields("merchant-application-status-query"))
                .extracting(field -> field.key())
                .containsExactly("apply_no", "huifu_id");

        TaskInputCatalog.PreparedInputs prepared = catalog.prepare("merchant-business-open",
                Map.of("huifu_id", "6661", "upper_huifu_id", "6660"), List.of());
        assertThat(prepared.arguments()).isEmpty();
        assertThat(prepared.runtimeInputs()).containsEntry("huifu_id", "6661");
    }

    @Test
    void pictureUploadRequiresExactlyOneImageSource() {
        assertThat(catalog.prepare("merchant-picture-upload",
                Map.of("file_path", "/workspace/input/a.png", "file_type", "F07"), List.of()).runtimeInputs())
                .containsEntry("file_path", "/workspace/input/a.png");
        assertThat(catalog.prepare("merchant-picture-upload",
                Map.of("file_url", "https://example.com/a.png", "file_type", "F07"), List.of()).runtimeInputs())
                .containsEntry("file_url", "https://example.com/a.png");
        assertThatThrownBy(() -> catalog.prepare("merchant-picture-upload", Map.of("file_type", "F07"), List.of()))
                .hasMessageContaining("必须且只能选择一种");
    }

    @Test
    void exposesAndValidatesStructuredJsonObjectFields() {
        assertThat(catalog.fields("merchant-business-open"))
                .filteredOn(field -> field.key().equals("sign_user_info") || field.key().equals("agreement_info"))
                .allMatch(field -> field.type().equals("JSON_OBJECT"));

        TaskInputCatalog.PreparedInputs prepared = catalog.prepare("merchant-business-open",
                Map.of("huifu_id", "6661", "upper_huifu_id", "6660", "sign_user_info", "{\"name\":\"张三\"}"),
                List.of());
        assertThat(prepared.runtimeInputs()).containsEntry("sign_user_info", "{\"name\":\"张三\"}");
        assertThatThrownBy(() -> catalog.prepare("merchant-business-open",
                Map.of("huifu_id", "6661", "upper_huifu_id", "6660", "sign_user_info", "[]"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON object");
    }
}
