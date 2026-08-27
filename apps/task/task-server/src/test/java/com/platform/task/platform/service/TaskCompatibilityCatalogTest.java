package com.platform.task.platform.service;

import com.platform.task.platform.repository.MerchantProfileRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TaskCompatibilityCatalogTest {
    @Test
    void exposesOnlyExplicitMainClassWhitelist() {
        TaskCompatibilityCatalog catalog = new TaskCompatibilityCatalog(mock(MerchantProfileRepository.class));
        assertThat(catalog.size()).isEqualTo(19);
        assertThat(catalog.requireMain("com.platform.task.controller.dji.V2MerchantBusiOpenTask")).isNotNull();
    }
}
