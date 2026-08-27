package com.platform.task.controller.hezhao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2MerchantBasicdataModifyRequest;
import com.huifu.bspay.sdk.opps.core.request.V2MerchantBasicdataStatusQueryRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Batch modify merchant settlement card.
 *
 * Default mode only executes the first merchant in Excel for verification.
 * Run with argument "batch" to execute all merchants by a fixed thread pool.
 * Run with argument "remaining" to skip the first merchant and execute the rest.
 */
public class V2MerchantBasicdataModifyTask {

    private static Path inputExcelPath() {
        return com.platform.task.controller.util.TaskPathResolver.path("TASK_INPUT_PATH",
                "D:\\dev\\code\\paas\\platform\\apps\\task\\task-server\\src\\main\\java\\com\\platform\\task\\controller\\hezhao",
                "\u548c\u5146\u5546\u6237\u5217\u8868.xlsx");
    }
    private static Path outputDirectory() {
        return com.platform.task.controller.util.TaskPathResolver.path(
                "TASK_OUTPUT_DIR", "D:\\dev\\code\\paas\\platform\\apps\\task\\task-server\\output");
    }
    private static final int SHEET_INDEX = 0;
    private static final int START_ROW_INDEX = 1;
    private static final int COL_HUIFU_ID = 1;
    private static final int THREAD_POOL_SIZE = 8;
    private static final String BATCH_ARG = "batch";
    private static final String REMAINING_ARG = "remaining";
    private static final String RESPONSE_HEADER = "response";
    private static final String APPLY_NO_HEADER = "apply_no";
    private static final String APPLY_QUERY_HEADER = "apply_query_response";

    public static void main(String[] args) throws Exception {
        V2MerchantBasicdataModifyTask task = new V2MerchantBasicdataModifyTask();
        task.init();

        List<MerchantRow> merchantRows = task.readMerchantRows();
        if (merchantRows.isEmpty()) {
            System.out.println("No huifu_id found in Excel: " + inputExcelPath());
            return;
        }

        boolean batchMode = args != null && args.length > 0 && BATCH_ARG.equalsIgnoreCase(args[0]);
        boolean remainingMode = args != null && args.length > 0 && REMAINING_ARG.equalsIgnoreCase(args[0]);
        System.out.println("Excel: " + inputExcelPath());
        System.out.println("Loaded huifu_id count: " + merchantRows.size());
        System.out.println("Run mode: " + ((batchMode || remainingMode)
                ? (remainingMode ? "remaining rows" : "batch") + ", threadPool=" + THREAD_POOL_SIZE
                : "first row only"));

        if (batchMode || remainingMode) {
            if (remainingMode) {
                merchantRows = merchantRows.subList(1, merchantRows.size());
                System.out.println("Skip first merchant. Remaining count: " + merchantRows.size());
            }
            task.executeBatch(merchantRows);
        } else {
            TaskResult result = task.executeOne(merchantRows.get(0));
            task.printResult(result);
            task.writeResults(List.of(result));
        }
    }

    private void init() throws Exception {
        MerConfig merConfig = new MerConfig();
        merConfig.setProductId(HeZhaoConfig.PRODUCT_ID);
        merConfig.setSysId(HeZhaoConfig.SYS_ID);
        merConfig.setRsaPrivateKey(HeZhaoConfig.RSA_PRIVATE_KEY);
        merConfig.setRsaPublicKey(HeZhaoConfig.RSA_PUBLIC_KEY);
        BasePay.initWithMerConfig(merConfig);
        BasePay.debug = false;
    }

    private List<MerchantRow> readMerchantRows() throws Exception {
        DataFormatter formatter = new DataFormatter();
        List<MerchantRow> result = new ArrayList<>();
        try (InputStream inputStream = new FileInputStream(inputExcelPath().toFile());
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(SHEET_INDEX);
            for (int i = START_ROW_INDEX; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String huifuId = formatter.formatCellValue(row.getCell(COL_HUIFU_ID)).trim();
                if (!huifuId.isEmpty()) {
                    result.add(new MerchantRow(i, huifuId));
                }
            }
        }
        return result;
    }

    private TaskResult executeOne(MerchantRow merchantRow) {
        try {
            V2MerchantBasicdataModifyRequest request = new V2MerchantBasicdataModifyRequest();
            request.setReqSeqId(SequenceTools.getReqSeqId32());
            request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
            request.setUpperHuifuId(HeZhaoConfig.SYS_ID);
            request.setHuifuId(merchantRow.huifuId());
            request.setExtendInfo(getExtendInfos());

            String requestJson = JSONObject.toJSONString(request);
            System.out.println("Submitting row=" + (merchantRow.rowIndex() + 1)
                    + ", huifu_id=" + merchantRow.huifuId());
            System.out.println("Request: " + requestJson);

            Map<String, Object> response = BasePayClient.request(request, false);
            String responseJson = JSONObject.toJSONString(response);
            String applyNo = findStringValue(response, "apply_no");
            String applyQueryResponse = applyNo == null || applyNo.isBlank()
                    ? ""
                    : queryApplication(merchantRow.huifuId(), applyNo);
            return new TaskResult(merchantRow.rowIndex(), merchantRow.huifuId(), requestJson, responseJson,
                    applyNo, applyQueryResponse, null);
        } catch (Exception e) {
            String error = e.getClass().getSimpleName() + ": " + e.getMessage();
            return new TaskResult(merchantRow.rowIndex(), merchantRow.huifuId(), null, error, null, null, error);
        }
    }

    private String queryApplication(String huifuId, String applyNo) throws Exception {
        V2MerchantBasicdataStatusQueryRequest request = new V2MerchantBasicdataStatusQueryRequest();
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setHuifuId(huifuId);
        request.setApplyNo(applyNo);
        Map<String, Object> response = BasePayClient.request(request, false);
        String responseJson = JSONObject.toJSONString(response);
        System.out.println("Application query huifu_id=" + huifuId + ", apply_no=" + applyNo
                + ", response=" + responseJson);
        return responseJson;
    }

    private void executeBatch(List<MerchantRow> merchantRows) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<TaskResult>> futures = new ArrayList<>();
        try {
            for (MerchantRow merchantRow : merchantRows) {
                futures.add(executorService.submit(new MerchantCallable(merchantRow)));
            }

            List<TaskResult> results = new ArrayList<>();
            for (Future<TaskResult> future : futures) {
                TaskResult result = future.get();
                results.add(result);
                printResult(result);
            }

            writeResults(results);
            System.out.println("Batch finished. Excel updated rows: " + results.size());
        } finally {
            executorService.shutdown();
        }
    }

    private void writeResults(List<TaskResult> results) throws Exception {
        Files.createDirectories(outputDirectory());
        Path outputPath = outputDirectory().resolve("hezhao-settlement-card-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx");
        try (InputStream inputStream = new FileInputStream(inputExcelPath().toFile());
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(SHEET_INDEX);
            int responseCol = findOrAppendHeader(sheet, RESPONSE_HEADER);
            int applyNoCol = findOrAppendHeader(sheet, APPLY_NO_HEADER);
            int applyQueryCol = findOrAppendHeader(sheet, APPLY_QUERY_HEADER);

            for (TaskResult result : results) {
                Row row = sheet.getRow(result.rowIndex());
                if (row == null) {
                    row = sheet.createRow(result.rowIndex());
                }
                row.createCell(responseCol).setCellValue(result.responseJson());
                row.createCell(applyNoCol).setCellValue(result.applyNo() == null ? "" : result.applyNo());
                row.createCell(applyQueryCol).setCellValue(
                        result.applyQueryResponse() == null ? "" : result.applyQueryResponse());
            }

            try (OutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
                workbook.write(outputStream);
            }
        }
        System.out.println("Excel write completed: " + outputPath);
    }

    private int findOrAppendHeader(Sheet sheet, String headerName) {
        Row header = sheet.getRow(0);
        if (header == null) {
            header = sheet.createRow(0);
        }

        short lastCellNum = header.getLastCellNum();
        int lastIndex = lastCellNum < 0 ? 0 : lastCellNum;
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < lastIndex; i++) {
            Cell cell = header.getCell(i);
            if (headerName.equals(formatter.formatCellValue(cell).trim())) {
                return i;
            }
        }

        header.createCell(lastIndex).setCellValue(headerName);
        return lastIndex;
    }

    private Map<String, Object> getExtendInfos() {
        Map<String, Object> extendInfoMap = new HashMap<>();
        extendInfoMap.put("card_info", getCardInfo());
        extendInfoMap.put("settle_config", getSettleConfig());
        return extendInfoMap;
    }

    private String getSettleConfig() {
        JSONObject dto = new JSONObject();
        dto.put("settle_status", "1");
        dto.put("settle_cycle", "T1");
        return dto.toJSONString();
    }

    private static String getCardInfo() {
        JSONObject dto = new JSONObject();
        dto.put("card_type", "0");
        dto.put("card_name", "\u4e0a\u6d77\u548c\u5146\u670d\u9970\u6709\u9650\u516c\u53f8");
        dto.put("card_no", "121923191410701");
        dto.put("prov_id", "310000");
        dto.put("area_id", "310100");
        dto.put("branch_code", "308290003597");
        dto.put("reg_acct_pic", "7482bb7d-1545-376f-a4a1-f1efea81bb2f");
        dto.put("bank_code", "03080000");
        dto.put("branch_name", "\u62db\u5546\u94f6\u884c\u80a1\u4efd\u6709\u9650\u516c\u53f8\u4e0a\u6d77\u5b9c\u5c71\u652f\u884c");
        return dto.toJSONString();
    }

    private void printResult(TaskResult result) {
        System.out.println("Result row=" + (result.rowIndex() + 1)
                + ", huifu_id=" + result.huifuId()
                + ", apply_no=" + (result.applyNo() == null ? "" : result.applyNo()));
        System.out.println("Response: " + result.responseJson());
        System.out.println("Application query response: " + result.applyQueryResponse());
        if (result.error() != null) {
            System.out.println("Error: " + result.error());
        }
    }

    @SuppressWarnings("unchecked")
    private static String findStringValue(Object source, String key) {
        if (source == null) {
            return null;
        }

        if (source instanceof Map<?, ?> map) {
            Object direct = map.get(key);
            if (direct != null) {
                return String.valueOf(direct);
            }

            for (Object value : map.values()) {
                String found = findStringValue(value, key);
                if (found != null && !found.isBlank()) {
                    return found;
                }
            }
        }

        if (source instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                try {
                    return findStringValue(JSON.parseObject(trimmed, Map.class), key);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private record MerchantRow(int rowIndex, String huifuId) {
    }

    private record TaskResult(int rowIndex, String huifuId, String requestJson, String responseJson, String applyNo,
                              String applyQueryResponse, String error) {
    }

    private class MerchantCallable implements Callable<TaskResult> {
        private final MerchantRow merchantRow;

        private MerchantCallable(MerchantRow merchantRow) {
            this.merchantRow = merchantRow;
        }

        @Override
        public TaskResult call() {
            return executeOne(merchantRow);
        }
    }
}
