package com.platform.task.controller.dji;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2UserBasicdataQueryRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;
import com.platform.task.controller.BaseController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息查询 - 大疆
 */
public class V2UserBasicdataQueryTask extends BaseController {

    static {
        System.setProperty("taskName", "V2UserBasicdataQueryTask");
    }



    // ==================== 待处理汇付商户号列表 ====================
    public static String[] HUIFU_IDS = {
            "6666000105447846"
    };

    @Override
    protected String getTaskName() {
        return "V2UserBasicdataQuery";
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
        V2UserBasicdataQueryRequest request = new V2UserBasicdataQueryRequest();
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setHuifuId(huifuId);
        request.setExtendInfo(getExtendInfos());

        String requestJson = JSONObject.toJSONString(request);
        log.info("{} Request: {}", huifuId, requestJson);

        Map<String, Object> response = BasePayClient.request(request);
        log.info("{} Response: {}", huifuId, JSONObject.toJSONString(response));

        return new TaskExecutionResult(requestJson, response);
    }

    private Map<String, Object> getExtendInfos() {
        return new HashMap<>();
    }

    public static void main(String[] args) throws Exception {
        new V2UserBasicdataQueryTask().execute(args);
    }
}
