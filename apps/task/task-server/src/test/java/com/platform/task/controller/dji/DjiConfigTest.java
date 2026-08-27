package com.platform.task.controller.dji;

import com.platform.task.controller.util.TaskExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DjiConfigTest {

    @Test
    void usesSelectedMerchantConfigurationDuringPlatformExecution() {
        try (TaskExecutionContext ignored = TaskExecutionContext.open(Map.of(
                "product_id", "PLATFORM_PRODUCT",
                "merchant.upper_huifu_id", "PLATFORM_UPPER"))) {
            assertThat(DjiConfig.value("product_id", "legacy")).isEqualTo("PLATFORM_PRODUCT");
            assertThat(DjiConfig.upperHuifuId()).isEqualTo("PLATFORM_UPPER");
        }
    }

    @Test
    void retainsLegacyDefaultsForDirectMainExecution() {
        assertThat(DjiConfig.value("product_id", DjiConfig.PRODUCT_ID)).isEqualTo(DjiConfig.PRODUCT_ID);
        assertThat(DjiConfig.upperHuifuId()).isEqualTo(DjiConfig.SYS_ID);
    }
}
