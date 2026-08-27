package com.platform.task.controller.cotti;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2TradePaymentScanpayRefundRequest;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scanpay refund batch task.
 *
 * Reads refund rows from cotti_refund.xlsx and writes refund_req_seq_id,
 * refund_req_date, refund_response and refund_trans_stat back to the same row.
 */
public class V2TradePaymentScanpayRefundRequestDemo {

    static {
        System.setProperty("taskName", "V2TradePaymentScanpayRefundRequestDemo");
    }

    private static Path inputExcelPath() {
        return com.platform.task.controller.util.TaskPathResolver.path("TASK_INPUT_PATH",
                "D:\\dev\\code\\paas\\platform\\apps\\task\\task-server\\src\\main\\java\\com\\platform\\task\\controller\\cotti",
                "cotti_refund.xlsx");
    }
    private static final int SHEET_INDEX = 0;

    private static final String COL_PAY_HF_SEQ_ID = "pay_hf_seq_id";
    private static final String COL_PAY_REQ_DATE = "pay_req_date";
    private static final String COL_HUIFU_ID = "huifu_id";
    private static final String COL_PAY_TRANS_STAT = "pay_trans_stat";
    private static final String COL_ORD_AMT = "ord_amt";
    private static final String COL_REFUND_REQ_SEQ_ID = "refund_req_seq_id";
    private static final String COL_REFUND_REQ_DATE = "refund_req_date";
    private static final String COL_REFUND_RESPONSE = "refund_response";
    private static final String COL_REFUND_TRANS_STAT = "refund_trans_stat";
    private static final boolean OVERWRITE_EXISTING_REFUND = true;

    public static void main(String[] args) throws Exception {
        new V2TradePaymentScanpayRefundRequestDemo().execute();
    }

    private void execute() throws Exception {
        doInit(getMerConfig());
        Path inputExcelPath = inputExcelPath();

        if (!Files.exists(inputExcelPath)) {
            throw new IllegalArgumentException("Input Excel not found: " + inputExcelPath);
        }

        int total = 0;
        int skipped = 0;
        int success = 0;
        int failed = 0;

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

                RefundRow refundRow = readRefundRow(row, columns, formatter);
                if (refundRow.isEmpty()) {
                    continue;
                }
                total++;

                String existingResponse = getCellString(row, columns.get(COL_REFUND_RESPONSE), formatter);
                if (isCurrentProductResponse(existingResponse)) {
                    skipped++;
                    continue;
                }
                if (!OVERWRITE_EXISTING_REFUND && !existingResponse.isBlank()) {
                    skipped++;
                    continue;
                }

                String payTransStat = refundRow.payTransStat;
                if (!payTransStat.isBlank() && !"S".equalsIgnoreCase(payTransStat)) {
                    skipped++;
                    writeRefundResult(workbook, row, columns, "", "",
                            "skip: pay_trans_stat=" + payTransStat, "");
                    saveWorkbook(workbook, inputExcelPath);
                    continue;
                }

                String refundReqDate = DateTools.getCurrentDateYYYYMMDD();
                String refundReqSeqId = SequenceTools.getReqSeqId32();

                try {
                    Map<String, Object> response = doRefund(refundRow, refundReqDate, refundReqSeqId);
                    String responseJson = JSONObject.toJSONString(response);
                    String refundTransStat = extractTransStat(response);
                    writeRefundResult(workbook, row, columns, refundReqSeqId, refundReqDate,
                            responseJson, refundTransStat);
                    saveWorkbook(workbook, inputExcelPath);
                    success++;
                    System.out.println("Row " + (i + 1) + " refund submitted: huifu_id=" + refundRow.huifuId
                            + ", ord_amt=" + refundRow.ordAmt + ", trans_stat=" + refundTransStat);
                } catch (Exception e) {
                    failed++;
                    writeRefundResult(workbook, row, columns, refundReqSeqId, refundReqDate,
                            "error: " + e.getMessage(), "");
                    saveWorkbook(workbook, inputExcelPath);
                    System.err.println("Row " + (i + 1) + " refund failed: huifu_id=" + refundRow.huifuId
                            + ", error=" + e.getMessage());
                }
            }
        }

        System.out.println("Refund batch finished. total=" + total
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

    private Map<String, Object> doRefund(RefundRow row, String refundReqDate, String refundReqSeqId)
            throws Exception {
        V2TradePaymentScanpayRefundRequest request = new V2TradePaymentScanpayRefundRequest();
        request.setReqDate(refundReqDate);
        request.setReqSeqId(refundReqSeqId);
        request.setHuifuId(row.huifuId);
        request.setOrdAmt(row.ordAmt);
        request.setOrgReqDate(row.payReqDate);
        request.setExtendInfo(getExtendInfos(row.payHfSeqId));

        String requestJson = JSONObject.toJSONString(request);
        System.out.println("Refund request: " + requestJson);

        Map<String, Object> response = BasePayClient.request(request, false);
        System.out.println("Refund response: " + JSONObject.toJSONString(response));
        return response;
    }

    private Map<String, Object> getExtendInfos(String payHfSeqId) {
        Map<String, Object> extendInfoMap = new HashMap<>();
        extendInfoMap.put("org_hf_seq_id", payHfSeqId);
        return extendInfoMap;
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

        ensureColumn(header, columns, COL_PAY_HF_SEQ_ID);
        ensureColumn(header, columns, COL_PAY_REQ_DATE);
        ensureColumn(header, columns, COL_HUIFU_ID);
        ensureColumn(header, columns, COL_ORD_AMT);
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

    private RefundRow readRefundRow(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        RefundRow refundRow = new RefundRow();
        refundRow.payHfSeqId = getCellString(row, columns.get(COL_PAY_HF_SEQ_ID), formatter);
        refundRow.payReqDate = getCellString(row, columns.get(COL_PAY_REQ_DATE), formatter);
        refundRow.huifuId = getCellString(row, columns.get(COL_HUIFU_ID), formatter);
        refundRow.payTransStat = getCellString(row, columns.getOrDefault(COL_PAY_TRANS_STAT, -1), formatter);
        refundRow.ordAmt = getCellString(row, columns.get(COL_ORD_AMT), formatter);

        if (!refundRow.isEmpty()) {
            requireNonBlank(refundRow.payHfSeqId, "pay_hf_seq_id", row.getRowNum());
            requireNonBlank(refundRow.payReqDate, "pay_req_date", row.getRowNum());
            requireNonBlank(refundRow.huifuId, "huifu_id", row.getRowNum());
            requireNonBlank(refundRow.ordAmt, "ord_amt", row.getRowNum());
        }
        return refundRow;
    }

    private void requireNonBlank(String value, String fieldName, int rowIndex) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is blank at Excel row " + (rowIndex + 1));
        }
    }

    private void writeRefundResult(Workbook workbook, Row row, Map<String, Integer> columns,
                                   String refundReqSeqId, String refundReqDate,
                                   String refundResponse, String refundTransStat) {
        setCellValue(row, columns.get(COL_REFUND_REQ_SEQ_ID), refundReqSeqId);
        setCellValue(row, columns.get(COL_REFUND_REQ_DATE), refundReqDate);
        setCellValue(row, columns.get(COL_REFUND_RESPONSE), refundResponse);
        setCellValue(row, columns.get(COL_REFUND_TRANS_STAT), refundTransStat);
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

    private boolean isCurrentProductResponse(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(response);
            Object productId = jsonObject.get("product_id");
            return CottiConfig.PRODUCT_ID.equals(productId);
        } catch (Exception e) {
            return response.contains("\"product_id\":\"" + CottiConfig.PRODUCT_ID + "\"")
                    || response.contains("\"product_id\": \"" + CottiConfig.PRODUCT_ID + "\"");
        }
    }

    private static final class RefundRow {
        String payHfSeqId;
        String payReqDate;
        String huifuId;
        String payTransStat;
        String ordAmt;

        boolean isEmpty() {
            return (payHfSeqId == null || payHfSeqId.isBlank())
                    && (payReqDate == null || payReqDate.isBlank())
                    && (huifuId == null || huifuId.isBlank())
                    && (ordAmt == null || ordAmt.isBlank());
        }
    }
}
