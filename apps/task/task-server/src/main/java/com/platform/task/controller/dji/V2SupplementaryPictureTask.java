package com.platform.task.controller.dji;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2SupplementaryPictureRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;
import com.platform.task.controller.BaseController;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 图片上传 - 大疆
 * 
 * 注意：图片上传的任务逻辑与普通 API 调用略有不同，huifuId 处记录文件路径。
 */
public class V2SupplementaryPictureTask extends BaseController {

    static {
        System.setProperty("taskName", "V2SupplementaryPictureTask");
    }



    // 默认待上传文件路径
    public static String[] FILE_PATHS = {
            com.platform.task.controller.util.TaskPathResolver.value("TASK_INPUT_PATH", "C:\\path\\to\\image.png")
    };

    @Override
    protected String getTaskName() {
        return "V2SupplementaryPicture";
    }

    @Override
    protected MerConfig getMerConfig() {
        return DjiConfig.merConfig();
    }

    @Override
    protected String[] getStaticHuifuIds() {
        return FILE_PATHS;
    }

    @Override
    protected TaskExecutionResult doExecute(String filePath) throws Exception {
        V2SupplementaryPictureRequest request = new V2SupplementaryPictureRequest();
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setFileType("F08"); // 默认类型

        String requestJson = JSONObject.toJSONString(request);
        log.info("上传文件: {} 请求: {}", filePath, requestJson);

        Map<String, Object> response = BasePayClient.upload(request, new File(filePath));
        log.info("响应: {}", JSONObject.toJSONString(response));

        return new TaskExecutionResult(requestJson, response);
    }

    public static void main(String[] args) throws Exception {
        new V2SupplementaryPictureTask().execute(args);
    }
}
