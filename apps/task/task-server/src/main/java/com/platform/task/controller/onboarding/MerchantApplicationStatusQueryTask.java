package com.platform.task.controller.onboarding;

import com.huifu.bspay.sdk.opps.core.request.V2MerchantBasicdataStatusQueryRequest;

public final class MerchantApplicationStatusQueryTask {
    private MerchantApplicationStatusQueryTask() {}
    public static void main(String[] args) throws Exception {
        MerchantApiTaskSupport.request(new V2MerchantBasicdataStatusQueryRequest());
    }
}
