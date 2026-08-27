package com.platform.task.controller.onboarding;

import com.huifu.bspay.sdk.opps.core.request.V2SupplementaryPictureRequest;

public final class MerchantPictureUploadTask {
    private MerchantPictureUploadTask() {}
    public static void main(String[] args) throws Exception {
        MerchantApiTaskSupport.upload(new V2SupplementaryPictureRequest());
    }
}
