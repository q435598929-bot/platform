package com.platform.task.controller.hezhao;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2SupplementaryPictureRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Supplementary picture upload task for HeZhao.
 */
public class V2SupplementaryPictureTask {

    static {
        System.setProperty("taskName", "V2SupplementaryPictureTask");
    }

    private static final String FILE_TYPE = "F08";
    public static void main(String[] args) throws Exception {
        new V2SupplementaryPictureTask().execute(args);
    }

    private void execute(String[] args) throws Exception {
        Path imagePath = args != null && args.length > 0 ? Path.of(args[0])
                : com.platform.task.controller.util.TaskPathResolver.path("TASK_INPUT_PATH",
                    "D:\\dev\\code\\paas\\platform\\apps\\task\\task-server\\src\\main\\java\\com\\platform\\task\\controller\\hezhao",
                    "c265f68f-4cd8-44ce-a34b-563dd65a759f.png");
        if (!Files.exists(imagePath)) {
            throw new IllegalArgumentException("Image file not found: " + imagePath);
        }

        doInit(getMerConfig());

        V2SupplementaryPictureRequest request = new V2SupplementaryPictureRequest();
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setFileType(FILE_TYPE);

        String requestJson = JSONObject.toJSONString(request);
        System.out.println("Upload request: " + requestJson);
        System.out.println("Upload image: " + imagePath);

        Map<String, Object> response = BasePayClient.upload(request, imagePath.toFile());
        String responseJson = JSONObject.toJSONString(response);
        String fileId = extractFileId(response);

        System.out.println("Upload response: " + responseJson);
        System.out.println("file_id=" + (fileId.isBlank() ? "<empty>" : fileId));
    }

    private MerConfig getMerConfig() {
        MerConfig merConfig = new MerConfig();
        merConfig.setProductId(HeZhaoConfig.PRODUCT_ID);
        merConfig.setSysId(HeZhaoConfig.SYS_ID);
        merConfig.setRsaPrivateKey(HeZhaoConfig.RSA_PRIVATE_KEY);
        merConfig.setRsaPublicKey(HeZhaoConfig.RSA_PUBLIC_KEY);
        return merConfig;
    }

    private void doInit(MerConfig merConfig) throws Exception {
        BasePay.initWithMerConfig(merConfig);
        BasePay.debug = false;
    }

    private String extractFileId(Map<String, Object> response) {
        if (response == null) {
            return "";
        }
        Object fileId = response.get("file_id");
        if (fileId == null) {
            Object data = response.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                fileId = dataMap.get("file_id");
                if (fileId == null) {
                    fileId = extractFileId((Map<String, Object>) dataMap);
                }
            } else if (data instanceof String dataString && !dataString.isBlank()) {
                fileId = extractFileId(JSONObject.parseObject(dataString));
            }
        }
        return fileId != null ? fileId.toString() : "";
    }
}
