package com.platform.task.controller.onboarding;

import com.huifu.bspay.sdk.opps.core.request.V2MerchantBasicdataEntRequest;

public final class EnterpriseMerchantOnboardingTask {
    private EnterpriseMerchantOnboardingTask() {}
    public static void main(String[] args) throws Exception {
        MerchantApiTaskSupport.request(new V2MerchantBasicdataEntRequest());
    }
}
