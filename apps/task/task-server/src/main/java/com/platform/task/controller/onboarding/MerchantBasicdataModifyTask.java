package com.platform.task.controller.onboarding;

import com.huifu.bspay.sdk.opps.core.request.V2MerchantBasicdataModifyRequest;

public final class MerchantBasicdataModifyTask {
    private MerchantBasicdataModifyTask() {}
    public static void main(String[] args) throws Exception {
        MerchantApiTaskSupport.request(new V2MerchantBasicdataModifyRequest());
    }
}
