package com.platform.task.controller.onboarding;

import com.platform.task.controller.util.TaskExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantApiTaskSupportTest {
    @Test
    void prefersCanonicalMerchantKeyName() {
        try (TaskExecutionContext ignored = TaskExecutionContext.open(Map.of(
                "merchant_private_key", "canonical-private-key",
                "rsa_private_key", "legacy-private-key"))) {
            assertThat(MerchantApiTaskSupport.requiredConfiguration(
                    "merchant_private_key", "rsa_private_key")).isEqualTo("canonical-private-key");
        }
    }

    @Test
    void readsLegacySdkKeyNameForCompatibility() {
        try (TaskExecutionContext ignored = TaskExecutionContext.open(Map.of(
                "merchant.rsa_public_key", "legacy-huifu-public-key"))) {
            assertThat(MerchantApiTaskSupport.requiredConfiguration(
                    "huifu_public_key", "rsa_public_key")).isEqualTo("legacy-huifu-public-key");
        }
    }

    @Test
    void exposesFileIdFromSdkNestedJsonString() {
        Map<String, Object> output = MerchantApiTaskSupport.normalizeUploadOutput(Map.of(
                "data", "{\"data\":{\"resp_desc\":\"成功\",\"file_id\":\"file-123\",\"resp_code\":\"00000000\"}}"));

        assertThat(output).containsEntry("file_id", "file-123");
        assertThat(output.get("data")).isEqualTo(
                "{\"data\":{\"resp_desc\":\"成功\",\"file_id\":\"file-123\",\"resp_code\":\"00000000\"}}");
    }
}
