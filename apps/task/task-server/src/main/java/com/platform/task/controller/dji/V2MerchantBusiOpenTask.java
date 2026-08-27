package com.platform.task.controller.dji;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2MerchantBusiOpenRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;
import com.platform.task.controller.BaseController;

import java.util.HashMap;
import java.util.Map;

/**
 * 商户业务开通 - 大疆
 * 继承自 BaseController，实现具体的业务逻辑。
 */
public class V2MerchantBusiOpenTask extends BaseController {

    static {
        System.setProperty("taskName", "V2MerchantBusiOpenTask");
    }

    public static String[] HUIFU_IDS = {
        "6666000211265177",
        "6666000211266350",
        "6666000212261955",
        "6666000213345424",
        "6666000219327723",
        "6666000214487111",
        "6666000218520539"
    };

    @Override
    protected String getTaskName() {
        return "V2MerchantBusiOpen";
    }

    @Override
    protected MerConfig getMerConfig() {
        return DjiConfig.merConfig();
    }

    @Override
    protected String[] getStaticHuifuIds() {
        return HUIFU_IDS;
    }

    @Override
    protected TaskExecutionResult doExecute(String huifuId) throws Exception {
        V2MerchantBusiOpenRequest request = new V2MerchantBusiOpenRequest();
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setHuifuId(huifuId);
        request.setUpperHuifuId(DjiConfig.upperHuifuId());
        request.setExtendInfo(getExtendInfos());

        String requestJson = JSONObject.toJSONString(request);
        log.info("{} Request: {}", huifuId, requestJson);

        Map<String, Object> response = BasePayClient.request(request);
        log.info("{} Response: {}", huifuId, JSONObject.toJSONString(response));

        return new TaskExecutionResult(requestJson, response);
    }

    private Map<String, Object> getExtendInfos() {
        Map<String, Object> m = new HashMap<>();
        m.put("half_pay_host_flag", "Y");
        m.put("agreement_info", getAgreementInfo());
        return m;
    }

    private String getAgreementInfo() {
        JSONObject dto = new JSONObject();
        dto.put("agreement_type", "3");
        dto.put("agreement_url",
                "https://cloudpnrcdn.oss-cn-shanghai.aliyuncs.com/opps/api/prod/download_file/PaymentServiceAgreement.htm");
        return dto.toJSONString();
    }

    public static void main(String[] args) throws Exception {
        new V2MerchantBusiOpenTask().execute(args);
    }
}
