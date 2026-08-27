package com.platform.task.controller.yuanzu;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2SupplementaryPictureRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;

import java.nio.file.Path;
import java.util.Map;

/** Uploads the Yuanzu supplementary file and prints the returned file_id. */
public class V2SupplementaryPictureTask {
    public static void main(String[] args) throws Exception {
        Path file = args != null && args.length > 0
                ? Path.of(args[0])
                : com.platform.task.controller.util.TaskPathResolver.path("TASK_INPUT_PATH",
                    "D:\\dev\\code\\paas\\platform\\apps\\task\\task-server\\src\\main\\java\\com\\platform\\task\\controller\\yuanzu\\yuanzu.txt");
        V2SupplementaryPictureRequest request = new V2SupplementaryPictureRequest();
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setFileType("F480");

        MerConfig config = new MerConfig();
        config.setProductId(YuanzuConfig.PRODUCT_ID);
        config.setSysId(YuanzuConfig.SYS_ID);
        config.setRsaPrivateKey(YuanzuConfig.RSA_PRIVATE_KEY);
        config.setRsaPublicKey(YuanzuConfig.RSA_PUBLIC_KEY);
        BasePay.initWithMerConfig(config);
        BasePay.debug = false;

        Map<String, Object> response = BasePayClient.upload(request, file.toFile());
        System.out.println(JSONObject.toJSONString(response));
        System.out.println("file_id=" + extractFileId(response));
    }

    @SuppressWarnings("unchecked")
    private static String extractFileId(Map<String, Object> response) {
        Object data = response.get("data");
        if (data instanceof String json) {
            Map<String, Object> payload = JSONObject.parseObject(json, Map.class);
            return extractFileId(payload);
        }
        if (data instanceof Map<?, ?> payload) {
            return extractFileId((Map<String, Object>) payload);
        }
        Object fileId = response.get("file_id");
        return fileId == null ? "" : fileId.toString();
    }
}
