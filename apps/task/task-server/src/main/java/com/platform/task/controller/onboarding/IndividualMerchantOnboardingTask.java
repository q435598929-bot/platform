package com.platform.task.controller.onboarding;

import com.huifu.bspay.sdk.opps.core.request.V2MerchantBasicdataIndvRequest;

public final class IndividualMerchantOnboardingTask {
    private IndividualMerchantOnboardingTask() {}
    public static void main(String[] args) throws Exception {
        MerchantApiTaskSupport.request(new V2MerchantBasicdataIndvRequest());
    }
}
