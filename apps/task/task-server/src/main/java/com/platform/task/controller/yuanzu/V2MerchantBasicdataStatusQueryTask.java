package com.platform.task.controller.yuanzu;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2MerchantBasicdataStatusQueryRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;

import java.util.Map;

/** Queries merchant basic-data application status for id:applyNo command-line pairs. */
public class V2MerchantBasicdataStatusQueryTask {
    public static void main(String[] args) throws Exception {
        MerConfig config = new MerConfig();
        config.setProductId(YuanzuConfig.PRODUCT_ID);
        config.setSysId(YuanzuConfig.SYS_ID);
        config.setRsaPrivateKey(YuanzuConfig.RSA_PRIVATE_KEY);
        config.setRsaPublicKey(YuanzuConfig.RSA_PUBLIC_KEY);
        BasePay.initWithMerConfig(config);
        BasePay.debug = false;

        for (String arg : args) {
            String[] pair = arg.split(":", 2);
            if (pair.length != 2) throw new IllegalArgumentException("Expected huifuId:applyNo, got " + arg);
            V2MerchantBasicdataStatusQueryRequest request = new V2MerchantBasicdataStatusQueryRequest();
            request.setReqSeqId(SequenceTools.getReqSeqId32());
            request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
            request.setHuifuId(pair[0]);
            request.setApplyNo(pair[1]);
            Map<String, Object> response = BasePayClient.request(request);
            System.out.println(pair[0] + "\t" + pair[1] + "\t" + JSONObject.toJSONString(response));
        }
    }
}
