package com.platform.task.controller.dji;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2MerchantBasicdataStatusQueryRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;
import com.platform.task.controller.BaseController;
import com.platform.task.controller.util.ExcelRecordService;
import com.platform.task.controller.util.ExcelRecordService.ExcelRow;

import java.util.List;
import java.util.Map;

/**
 * 商户基本信息申请状态查询 - 大疆
 * 
 * 该任务从 Excel 中读取带 apply_no 的行，查询状态并回写。
 */
public class V2MerchantBasicdataStatusQueryTask extends BaseController {

    static {
        System.setProperty("taskName", "V2MerchantBasicdataStatusQueryTask");
    }

    @Override
    protected String getTaskName() {
        return "V2MerchantBasicdataStatusQuery";
    }

    @Override
    protected MerConfig getMerConfig() {
        return DjiConfig.merConfig();
    }

    @Override
    protected String[] getStaticHuifuIds() {
        return new String[0]; // 查询类通常从 Excel 读取，不依赖静态 ID
    }

    /**
     * 重写 execute 方法，因为查询逻辑与普通提交逻辑不同：
     * 普通逻辑是 process by huifuId -> append record
     * 查询逻辑是 read from Excel -> query by apply_no -> update record
     */
    @Override
    public void execute(String[] args) throws Exception {
        // 1. 初始化 SDK
        BasePay.initWithMerConfig(getMerConfig());

        // 2. 确定要处理的 Excel 文件
        List<String> excelFiles = ExcelRecordService.findExcelFilesWithPendingRows();
        if (excelFiles.isEmpty()) {
            log.warn("未找到含有待查询 apply_no 的 Excel 文件");
            return;
        }

        for (String excelPath : excelFiles) {
            processExcelFile(excelPath);
        }
    }

    private void processExcelFile(String excelPath) throws Exception {
        List<ExcelRow> pendingRows = ExcelRecordService.readPendingRows(excelPath);
        log.info("处理文件: {}, 待查询数: {}", excelPath, pendingRows.size());

        for (ExcelRow row : pendingRows) {
            try {
                String applyResult = doQuery(row.huifuId, row.applyNo);
                ExcelRecordService.updateApplyResult(excelPath, row.applyNo, applyResult);
                log.info("{} apply_no={} 查询成功", row.huifuId, row.applyNo);
            } catch (Exception e) {
                log.error("{} apply_no={} 查询失败: {}", row.huifuId, row.applyNo, e.getMessage());
            }
        }
    }

    private String doQuery(String huifuId, String applyNo) throws Exception {
        V2MerchantBasicdataStatusQueryRequest request = new V2MerchantBasicdataStatusQueryRequest();
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setHuifuId(huifuId);
        request.setApplyNo(applyNo);

        Map<String, Object> response = BasePayClient.request(request);
        return extractApplyResult(response);
    }

    private String extractApplyResult(Map<String, Object> response) {
        if (response == null) return "响应为空";
        
        String applyStatus = (String) response.get("apply_status");
        String respDesc    = (String) response.get("resp_desc");
        String respCode    = (String) response.get("resp_code");

        if (applyStatus == null || applyStatus.isEmpty()) {
            Object data = response.get("data");
            if (data instanceof Map) {
                Map<?, ?> dataMap = (Map<?, ?>) data;
                applyStatus = (String) dataMap.get("apply_status");
                if (respDesc == null || respDesc.isEmpty()) respDesc = (String) dataMap.get("resp_desc");
            }
        }

        StringBuilder sb = new StringBuilder();
        if (respCode != null) sb.append("code=").append(respCode).append(" ");
        if (respDesc != null) sb.append("desc=").append(respDesc).append(" ");
        if (applyStatus != null) sb.append("status=").append(applyStatus);

        return sb.toString().trim();
    }

    @Override
    protected TaskExecutionResult doExecute(String huifuId) throws Exception {
        // 对于查询任务，主要逻辑在 execute 中重写了，此处留空
        return null;
    }

    public static void main(String[] args) throws Exception {
        new V2MerchantBasicdataStatusQueryTask().execute(args);
    }
}
