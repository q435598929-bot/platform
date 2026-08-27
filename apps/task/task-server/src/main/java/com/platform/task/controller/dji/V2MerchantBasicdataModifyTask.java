package com.platform.task.controller.dji;

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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merchant basic data modify task.
 *
 * Reads huifu_id from Excel column D and ext_mer_id from column F, then submits
 * ext_mer_id in extendInfo for each merchant.
 */
public class V2MerchantBasicdataModifyTask extends BaseController {

    static {
        System.setProperty("taskName", "V2MerchantBasicdataModifyTask");
    }

    private static String inputExcelPath() {
        return com.platform.task.controller.util.TaskPathResolver.value(
                "TASK_INPUT_PATH", "C:\\Users\\ronggui.liao_c\\Desktop\\大疆外部商户号批量配置0507.xlsx");
    }
    private static final int COL_HUIFU_ID = 3;   // D
    private static final int COL_EXT_MER_ID = 5; // F
    private final Map<String, String> extMerIds = loadExtMerIds();

    public static String[] HUIFU_IDS = {
            // Fallback only. The Excel file above is used when it has valid rows.
            "6666000195065396"
    };

    @Override
    protected String getTaskName() {
        return "V2MerchantBasicdataModify";
    }

    @Override
    protected MerConfig getMerConfig() {
        return DjiConfig.merConfig();
    }

    @Override
    protected String[] getStaticHuifuIds() {
        if (!extMerIds.isEmpty()) {
            return extMerIds.keySet().toArray(new String[0]);
        }

        System.out.println(extMerIds);
        return null;
        // return HUIFU_IDS;
    }

    @Override
    protected TaskExecutionResult doExecute(String huifuId) throws Exception {
        String extMerId = extMerIds.get(huifuId);
        if (extMerId == null || extMerId.isBlank()) {
            throw new IllegalArgumentException("ext_mer_id not found for huifu_id: " + huifuId);
        }

        V2MerchantBasicdataModifyRequest request = new V2MerchantBasicdataModifyRequest();
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setUpperHuifuId(DjiConfig.upperHuifuId());
        request.setHuifuId(huifuId);
        request.setExtendInfo(getExtendInfos(extMerId));

        String requestJson = JSONObject.toJSONString(request);
        log.info("{} Request: {}", huifuId, requestJson);

        Map<String, Object> response = BasePayClient.request(request, false);
        log.info("{} Response: {}", huifuId, JSONObject.toJSONString(response));

        return new TaskExecutionResult(requestJson, response);
    }

    private Map<String, Object> getExtendInfos(String extMerId) {
        Map<String, Object> extendInfoMap = new HashMap<>();
        extendInfoMap.put("ext_mer_id", extMerId);
        return extendInfoMap;
    }

    private static Map<String, String> loadExtMerIds() {
        Map<String, String> result = new LinkedHashMap<>();
        Path path = Path.of(inputExcelPath());
        if (!Files.exists(path)) {
            System.err.println("Input Excel not found: " + inputExcelPath());
            return result;
        }

        DataFormatter formatter = new DataFormatter();
        try (InputStream is = new FileInputStream(path.toFile());
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String huifuId = formatter.formatCellValue(row.getCell(COL_HUIFU_ID)).trim();
                String extMerId = formatter.formatCellValue(row.getCell(COL_EXT_MER_ID)).trim();
                if (!huifuId.isEmpty() && !extMerId.isEmpty()) {
                    result.put(huifuId, extMerId);
                }
            }
            System.out.println("Loaded ext_mer_id mappings: " + result.size());
        } catch (Exception e) {
            System.err.println("Read input Excel failed: " + e.getMessage());
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        new V2MerchantBasicdataModifyTask().execute(args);
    }
}
