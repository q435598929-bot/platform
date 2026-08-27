package com.platform.task.platform.service;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantConfigurationCatalogTest {
    private static Map<String, String> validConfiguration() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("sys_id", "sys");
        values.put("huifu_id", "merchant");
        values.put("product_id", "product");
        values.put("huifu_public_key", "huifu-public");
        values.put("merchant_public_key", "merchant-public");
        values.put("merchant_private_key", "merchant-private");
        return values;
    }

    @Test
    void acceptsOnlyCanonicalKeysAndKeepsCatalogOrder() {
        assertThat(MerchantConfigurationCatalog.normalize(validConfiguration()).keySet())
                .containsExactly("sys_id", "huifu_id", "product_id", "huifu_public_key",
                        "merchant_public_key", "merchant_private_key");
    }

    @Test
    void rejectsIncorrectCapitalization() {
        Map<String, String> values = validConfiguration();
        values.put("Prod_id", "wrong");
        assertThatThrownBy(() -> MerchantConfigurationCatalog.normalize(values))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prod_id");
    }

    @Test
    void reportsMissingRequiredFieldWithBusinessLabel() {
        Map<String, String> values = validConfiguration();
        values.remove("product_id");
        assertThatThrownBy(() -> MerchantConfigurationCatalog.normalize(values))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("产品号");
    }
}
