package com.platform.task.controller.cotti;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2TradePaymentScanpayRefundqueryRequest;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scanpay refund query batch task.
 *
 * Reads refund_req_seq_id/refund_req_date/huifu_id from cotti_refund.xlsx,
 * queries refund status and updates refund_trans_stat in the same row.
 */
public class V2TradePaymentScanpayRefundqueryTask {

    static {
        System.setProperty("taskName", "V2TradePaymentScanpayRefundqueryTask");
    }

    private static Path inputExcelPath() {
        return com.platform.task.controller.util.TaskPathResolver.path("TASK_INPUT_PATH",
                "D:\\dev\\code\\paas\\platform\\apps\\task\\task-server\\src\\main\\java\\com\\platform\\task\\controller\\cotti",
                "cotti_refund.xlsx");
    }
    private static final int SHEET_INDEX = 0;

    private static final String COL_HUIFU_ID = "huifu_id";
    private static final String COL_REFUND_REQ_SEQ_ID = "refund_req_seq_id";
    private static final String COL_REFUND_REQ_DATE = "refund_req_date";
    private static final String COL_REFUND_RESPONSE = "refund_response";
    private static final String COL_REFUND_TRANS_STAT = "refund_trans_stat";

    public static void main(String[] args) throws Exception {
        new V2TradePaymentScanpayRefundqueryTask().execute();
    }

    private void execute() throws Exception {
        doInit(getMerConfig());
        Path inputExcelPath = inputExcelPath();

        if (!Files.exists(inputExcelPath)) {
            throw new IllegalArgumentException("Input Excel not found: " + inputExcelPath);
        }

        int total = 0;
        int success = 0;
        int failed = 0;
        int skipped = 0;
        boolean updated = false;

        Workbook workbook;
        try (InputStream is = new FileInputStream(inputExcelPath.toFile())) {
            workbook = new XSSFWorkbook(is);
        }

        try (workbook) {
            Sheet sheet = workbook.getSheetAt(SHEET_INDEX);
            Map<String, Integer> columns = resolveColumns(sheet);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                QueryRow queryRow = readQueryRow(row, columns, formatter);
                if (queryRow.isEmpty()) {
                    continue;
                }
                total++;

                if (queryRow.huifuId.isBlank()
                        || queryRow.refundReqSeqId.isBlank()
                        || queryRow.refundReqDate.isBlank()) {
                    skipped++;
                    System.err.println("Row " + (i + 1) + " skipped: missing huifu_id/refund_req_seq_id/refund_req_date");
                    continue;
                }

                try {
                    Map<String, Object> response = doQuery(queryRow);
                    String refundTransStat = extractTransStat(response);
                    if (!refundTransStat.isBlank()) {
                        setCellValue(row, columns.get(COL_REFUND_TRANS_STAT), refundTransStat);
                        updated = true;
                    }
                    success++;
                    System.out.println("Row " + (i + 1) + " refund query: huifu_id=" + queryRow.huifuId
                            + ", refund_req_seq_id=" + queryRow.refundReqSeqId
                            + ", trans_stat=" + (refundTransStat.isBlank() ? "<empty>" : refundTransStat));
                } catch (Exception e) {
                    failed++;
                    System.err.println("Row " + (i + 1) + " refund query failed: huifu_id=" + queryRow.huifuId
                            + ", refund_req_seq_id=" + queryRow.refundReqSeqId
                            + ", error=" + e.getMessage());
                }
            }

            if (updated) {
                saveWorkbook(workbook, inputExcelPath);
            }
        }

        System.out.println("Refund query batch finished. total=" + total
                + ", success=" + success + ", failed=" + failed + ", skipped=" + skipped);
    }

    private MerConfig getMerConfig() {
        MerConfig merConfig = new MerConfig();
        merConfig.setProductId(CottiConfig.PRODUCT_ID);
        merConfig.setSysId(CottiConfig.SYS_ID);
        merConfig.setRsaPrivateKey(CottiConfig.RSA_PRIVATE_KEY);
        merConfig.setRsaPublicKey(CottiConfig.RSA_PUBLIC_KEY);
        return merConfig;
    }

    private void doInit(MerConfig merConfig) throws Exception {
        BasePay.initWithMerConfig(merConfig);
        BasePay.debug = false;
    }

    private Map<String, Object> doQuery(QueryRow row) throws Exception {
        V2TradePaymentScanpayRefundqueryRequest request = new V2TradePaymentScanpayRefundqueryRequest();
        request.setHuifuId(row.huifuId);
        request.setOrgReqDate(row.refundReqDate);
        request.setOrgReqSeqId(row.refundReqSeqId);
        request.setOrgHfSeqId(row.refundHfSeqId);
        request.setMerOrdId("");

        String requestJson = JSONObject.toJSONString(request);
        System.out.println("Refund query request: " + requestJson);

        Map<String, Object> response = BasePayClient.request(request, false);
        System.out.println("Refund query response: " + JSONObject.toJSONString(response));
        return response;
    }

    private Map<String, Integer> resolveColumns(Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header == null) {
            header = sheet.createRow(0);
        }

        Map<String, Integer> columns = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();
        short lastCellNum = header.getLastCellNum();
        int lastCol = lastCellNum > 0 ? lastCellNum : 0;
        for (int i = 0; i < lastCol; i++) {
            String name = formatter.formatCellValue(header.getCell(i)).trim();
            if (!name.isEmpty()) {
                columns.put(name, i);
            }
        }

        ensureColumn(header, columns, COL_HUIFU_ID);
        ensureColumn(header, columns, COL_REFUND_REQ_SEQ_ID);
        ensureColumn(header, columns, COL_REFUND_REQ_DATE);
        ensureColumn(header, columns, COL_REFUND_RESPONSE);
        ensureColumn(header, columns, COL_REFUND_TRANS_STAT);
        return columns;
    }

    private void ensureColumn(Row header, Map<String, Integer> columns, String columnName) {
        if (columns.containsKey(columnName)) {
            return;
        }

        int col = header.getLastCellNum();
        if (col < 0) {
            col = 0;
        }
        header.createCell(col).setCellValue(columnName);
        columns.put(columnName, col);
    }

    private QueryRow readQueryRow(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        QueryRow queryRow = new QueryRow();
        queryRow.huifuId = getCellString(row, columns.get(COL_HUIFU_ID), formatter);
        queryRow.refundReqSeqId = getCellString(row, columns.get(COL_REFUND_REQ_SEQ_ID), formatter);
        queryRow.refundReqDate = getCellString(row, columns.get(COL_REFUND_REQ_DATE), formatter);
        queryRow.refundHfSeqId = extractRefundHfSeqId(getCellString(row, columns.get(COL_REFUND_RESPONSE), formatter));
        return queryRow;
    }

    private String extractRefundHfSeqId(String refundResponse) {
        if (refundResponse == null || refundResponse.isBlank()) {
            return "";
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(refundResponse);
            Object hfSeqId = jsonObject.get("hf_seq_id");
            return hfSeqId != null ? hfSeqId.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String extractTransStat(Map<String, Object> response) {
        if (response == null) {
            return "";
        }
        Object transStat = response.get("trans_stat");
        if (transStat == null) {
            transStat = response.get("refund_trans_stat");
        }
        if (transStat == null) {
            Object data = response.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                transStat = dataMap.get("trans_stat");
                if (transStat == null) {
                    transStat = dataMap.get("refund_trans_stat");
                }
            }
        }
        return transStat != null ? transStat.toString() : "";
    }

    private void saveWorkbook(Workbook workbook, Path inputExcelPath) throws Exception {
        try (OutputStream os = new FileOutputStream(inputExcelPath.toFile())) {
            workbook.write(os);
        }
    }

    private void setCellValue(Row row, int colIdx, String value) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        cell.setCellValue(value != null ? value : "");
    }

    private String getCellString(Row row, int colIdx, DataFormatter formatter) {
        if (row == null || colIdx < 0) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(colIdx)).trim();
    }

    private static final class QueryRow {
        String huifuId;
        String refundReqSeqId;
        String refundReqDate;
        String refundHfSeqId;

        boolean isEmpty() {
            return (huifuId == null || huifuId.isBlank())
                    && (refundReqSeqId == null || refundReqSeqId.isBlank())
                    && (refundReqDate == null || refundReqDate.isBlank());
        }
    }
}
