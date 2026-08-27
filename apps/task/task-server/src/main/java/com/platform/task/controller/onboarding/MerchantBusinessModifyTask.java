package com.platform.task.controller.onboarding;

import com.huifu.bspay.sdk.opps.core.request.V2MerchantBusiModifyRequest;

public final class MerchantBusinessModifyTask {
    private MerchantBusinessModifyTask() {}
    public static void main(String[] args) throws Exception {
        MerchantApiTaskSupport.request(new V2MerchantBusiModifyRequest());
    }
}
