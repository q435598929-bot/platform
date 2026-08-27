package com.platform.task.controller.onboarding;

import com.huifu.bspay.sdk.opps.core.request.V2MerchantBusiOpenRequest;

public final class MerchantBusinessOpenTask {
    private MerchantBusinessOpenTask() {}
    public static void main(String[] args) throws Exception {
        MerchantApiTaskSupport.request(new V2MerchantBusiOpenRequest());
    }
}
