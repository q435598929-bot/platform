package com.platform.task.controller.quhulian;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2MerchantBasicdataModifyRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;
import com.platform.task.controller.BaseController;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads huifu_id from Sheet2 column A, modifies merchant MCC to 8299, then
 * writes the synchronous response and apply_no back to Sheet2 columns C and D.
 */
public class V2MerchantBasicdataModifyTask extends BaseController {

    static {
        System.setProperty("taskName", "V2MerchantBasicdataModifyTask");
    }

    private static Path inputExcelPath() {
        return com.platform.task.controller.util.TaskPathResolver.path("TASK_INPUT_PATH",
                "D:\\dev\\code\\paas\\platform\\apps\\task\\task-server\\src\\main\\java\\com\\platform\\task\\controller\\quhulian",
                "\u8da3\u4e92\u8054\u6821\u56ed\u5546\u6237\u6e05\u5355\uff08\u6700\u7ec8\u7248\u6c47\u4ed8\u5f52\u6863\uff09.xlsx");
    }
    private static final int SHEET_INDEX = 1; // Sheet2
    private static final int COL_HUIFU_ID = 0; // A
    private static final int COL_SYNC_RESPONSE = 2; // C
    private static final int COL_APPLY_NO = 3; // D
    private static final Object INPUT_EXCEL_LOCK = new Object();
    private final Map<String, Integer> huifuIdRows = loadHuifuIdRows();

    @Override
    protected String getTaskName() {
        return "V2MerchantBasicdataModify";
    }

    @Override
    protected MerConfig getMerConfig() {
        MerConfig merConfig = new MerConfig();
        merConfig.setProductId(QhlConfig.PRODUCT_ID);
        merConfig.setSysId(QhlConfig.SYS_ID);
        merConfig.setRsaPrivateKey(QhlConfig.RSA_PRIVATE_KEY);
        merConfig.setRsaPublicKey(QhlConfig.RSA_PUBLIC_KEY);
        return merConfig;
    }

    @Override
    protected String[] getStaticHuifuIds() {
        if (!huifuIdRows.isEmpty()) {
            return huifuIdRows.keySet().toArray(new String[0]);
        }

        return null;
    }

    @Override
    public void execute(String[] args) throws Exception {
        System.out.println("Quhulian MCC modify task will read Sheet2 huifu_id count: " + huifuIdRows.size());
        super.execute(new String[0]);
    }

    @Override
    protected TaskExecutionResult doExecute(String huifuId) throws Exception {
        if (!huifuIdRows.containsKey(huifuId)) {
            throw new IllegalArgumentException("huifu_id not found in Sheet2: " + huifuId);
        }

        V2MerchantBasicdataModifyRequest request = new V2MerchantBasicdataModifyRequest();
        System.out.println("[" + huifuId + "] submitting MCC modify request, mcc=8299");
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setUpperHuifuId(QhlConfig.SYS_ID);
        request.setHuifuId(huifuId);
        request.setExtendInfo(getExtendInfos());

        String requestJson = JSONObject.toJSONString(request);
        log.info("{} Request: {}", huifuId, requestJson);

        Map<String, Object> response = BasePayClient.request(request, false);
        String responseJson = JSONObject.toJSONString(response);
        String applyNo = extractApplyNo(response);
        log.info("{} Response: {}", huifuId, responseJson);
        System.out.println("[" + huifuId + "] response received, apply_no=" + (applyNo.isBlank() ? "<empty>" : applyNo));
        writeSyncResult(huifuId, responseJson, applyNo);

        return new TaskExecutionResult(requestJson, response);
    }

    private Map<String, Object> getExtendInfos() {
        Map<String, Object> extendInfoMap = new HashMap<>();
        extendInfoMap.put("mcc", 8299);
        return extendInfoMap;
    }

    private static Map<String, Integer> loadHuifuIdRows() {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!Files.exists(inputExcelPath())) {
            System.err.println("Input Excel not found: " + inputExcelPath());
            return result;
        }

        DataFormatter formatter = new DataFormatter();
        try (InputStream is = new FileInputStream(inputExcelPath().toFile());
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(SHEET_INDEX);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String huifuId = formatter.formatCellValue(row.getCell(COL_HUIFU_ID)).trim();
                String syncResponse = formatter.formatCellValue(row.getCell(COL_SYNC_RESPONSE)).trim();
                if (!huifuId.isEmpty() && syncResponse.isEmpty()) {
                    result.put(huifuId, i);
                }
            }
            System.out.println("Loaded Sheet2 pending huifu_id rows: " + result.size());
        } catch (Exception e) {
            System.err.println("Read Sheet2 huifu_id failed: " + e.getMessage());
        }
        return result;
    }

    private void writeSyncResult(String huifuId, String responseJson, String applyNo) throws Exception {
        Integer rowIndex = huifuIdRows.get(huifuId);
        if (rowIndex == null) {
            throw new IllegalArgumentException("huifu_id row not found in Sheet2: " + huifuId);
        }

        synchronized (INPUT_EXCEL_LOCK) {
            try (InputStream is = new FileInputStream(inputExcelPath().toFile());
                 Workbook workbook = new XSSFWorkbook(is)) {
                Sheet sheet = workbook.getSheetAt(SHEET_INDEX);
                ensureResultHeaders(sheet);

                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    row = sheet.createRow(rowIndex);
                }
                row.createCell(COL_SYNC_RESPONSE).setCellValue(responseJson != null ? responseJson : "");
                row.createCell(COL_APPLY_NO).setCellValue(applyNo != null ? applyNo : "");

                try (OutputStream os = new FileOutputStream(inputExcelPath().toFile())) {
                    workbook.write(os);
                }
                System.out.println("[" + huifuId + "] Sheet2 row " + (rowIndex + 1)
                        + " updated: response column C, apply_no column D");
            }
        }
    }

    private void ensureResultHeaders(Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header == null) {
            header = sheet.createRow(0);
        }
        if (header.getCell(COL_SYNC_RESPONSE) == null) {
            header.createCell(COL_SYNC_RESPONSE).setCellValue("\u4fee\u6539\u540c\u6b65\u8fd4\u56de\u6570\u636e");
        }
        if (header.getCell(COL_APPLY_NO) == null) {
            header.createCell(COL_APPLY_NO).setCellValue("\u7533\u8bf7\u5355\u53f7");
        }
    }

    private String extractApplyNo(Map<String, Object> response) {
        if (response == null) {
            return "";
        }
        Object applyNo = response.get("apply_no");
        if (applyNo == null) {
            Object data = response.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                applyNo = dataMap.get("apply_no");
            }
        }
        return applyNo != null ? applyNo.toString() : "";
    }

    public static void main(String[] args) throws Exception {
        new V2MerchantBasicdataModifyTask().execute(args);
    }
}
