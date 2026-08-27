package com.platform.task.controller.hema;

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
        return com.platform.task.controller.util.TaskPathResolver.value("TASK_INPUT_PATH", "");
    }
    private static String settleCardExcelPath() {
        return com.platform.task.controller.util.TaskPathResolver.value(
                "TASK_AUX_INPUT_PATH", "C:\\Users\\ronggui.liao_c\\Desktop\\hema_settle_cards.xlsx");
    }
    private static final int COL_HUIFU_ID = 3;   // D
    private static final int COL_EXT_MER_ID = 5; // F
    private static final int COL_CARD_HUIFU_ID = 0;    // A
    private static final int COL_CARD_TYPE = 1;        // B
    private static final int COL_CARD_NO = 2;          // C
    private static final int COL_CARD_NAME = 3;        // D
    private static final int COL_CARD_BRANCH_CODE = 7; // H
    private static final int COL_CARD_PROV_ID = 8;     // I
    private static final int COL_CARD_AREA_ID = 9;     // J
    private final Map<String, String> extMerIds = loadExtMerIds();
    private final Map<String, CardInfo> settleCards = loadSettleCards();

    public static String[] HUIFU_IDS = {
        // Fallback only. The Excel file above is used when it has valid rows.
        "6666000193460096",
        // "6666000193460034",
        // "6666000193459899",
        // "6666000193064350",
        // "6666000193064303",
        // "6666000193064263",
        // "6666000193064246",
        // "6666000193064164",
        // "6666000193063982",
        // "6666000193063963",
        // "6666000193063920",
        // "6666000193063808",
        // "6666000193041297",
        // "6666000192726731",
        // "6666000192726571",
        // "6666000192641409",
        // "6666000192641221",
        // "6666000192640646",
        // "6666000192640414",
        // "6666000192639995",
        // "6666000192639798",
        // "6666000192639466",
        // "6666000192638911",
        // "6666000192638761",
        // "6666000192638228",
        // "6666000192638071",
        // "6666000192637645",
        // "6666000192637294",
        // "6666000192564981",
        // "6666000192564666",
        // "6666000192561989",
        // "6666000192561537",
        // "6666000192561311",
        // "6666000192560675",
        // "6666000192560476",
        // "6666000192518760",
        // "6666000192518500",
        // "6666000192516474",
        // "6666000192516265",
        // "6666000192515393",
        // "6666000192514699",
        // "6666000192506974",
        // "6666000192506195",
        // "6666000192504872",
        // "6666000192503846",
        // "6666000192499607",
        // "6666000192498372",
        // "6666000192497034",
        // "6666000192410828",
        // "6666000192410517",
        // "6666000192410041",
        // "6666000192409684",
        // "6666000192402338",
        // "6666000192400396",
        // "6666000192398911",
        // "6666000192397611",
        // "6666000192396813",
        // "6666000192394002",
        // "6666000192393487",
        // "6666000192390965",
        // "6666000192371977",
        // "6666000192371977",
        // "6666000191379767",
        // "6666000191379760",
        // "6666000191379703",
        // "6666000191379699",
        // "6666000191379673",
        // "6666000191379637",
        // "6666000191230197",
        // "6666000191229981",
        // "6666000191229605",
        // "6666000191228952",
        // "6666000191228385",
        // "6666000191228187",
        // "6666000191190272",
        // "6666000191190163",
        // "6666000191189237",
        // "6666000191189062",
        // "6666000191156229",
        // "6666000191156028",
        // "6666000191155503",
        // "6666000191155311",
        // "6666000191154297",
        // "6666000191153834",
        // "6666000191153442",
        // "6666000191152544",
        // "6666000191152273",
        // "6666000191151551",
        // "6666000191151207",
        // "6666000191150215",
        // "6666000191149203",
        // "6666000191147851",
        // "6666000191147469",
        // "6666000191146952",
        // "6666000191146727",
        // "6666000191095633",
        // "6666000191095213",
        // "6666000191089553",
        // "6666000191088918",
        // "6666000191086030",
        // "6666000191067166",
        // "6666000191065739",
        // "6666000191063840",
        // "6666000191062993",
        // "6666000190036772",
        // "6666000189982799",
        // "6666000189596094",
        // "6666000189591822",
        // "6666000189590463",
        // "6666000189586917",
        // "6666000189584825",
        // "6666000189583616",
        // "6666000189582698",
        // "6666000189581312",
        // "6666000189579325",
        // "6666000189576836",
        // "6666000189481014",
        // "6666000189479999",
        // "6666000189477785",
        // "6666000189476881",
        // "6666000189385858",
        // "6666000189023098",
        // "6666000188855627",
        // "6666000188855587",
        // "6666000188853921",
        // "6666000188853871",
        // "6666000188850390",
        // "6666000188850288",
        // "6666000188850226",
        // "6666000188850148",
        // "6666000188850064",
        // "6666000188849974",
        // "6666000188849843",
        // "6666000188849653",
        // "6666000188834212",
        // "6666000188834061",
        // "6666000188833912",
        // "6666000188833770",
        // "6666000188833652",
        // "6666000188833446",
        // "6666000188833262",
        // "6666000188833089",
        // "6666000188832933",
        // "6666000188832738",
        // "6666000188832512",
        // "6666000188832324",
        // "6666000188832080",
        // "6666000188831876",
        // "6666000188831755",
        // "6666000188831494",
        // "6666000188831285",
        // "6666000188830776",
        // "6666000188830530",
        // "6666000188830003",
        // "6666000188829682",
        // "6666000188821817",
        // "6666000188821557",
        // "6666000188821288",
        // "6666000188821067",
        // "6666000188820789",
        // "6666000188820521",
        // "6666000188820217",
        // "6666000188819919",
        // "6666000188819366",
        // "6666000188819092",
        // "6666000188818765",
        // "6666000188817734",
        // "6666000188817220",
        // "6666000188816699",
        // "6666000188816428",
        // "6666000188816014",
        // "6666000188815604",
        // "6666000188814950",
        // "6666000188814103",
        // "6666000188813392",
        // "6666000188812719",
        // "6666000188071741",
        // "6666000188068691",
        // "6666000188068043",
        // "6666000188067735",
        // "6666000188067267",
        // "6666000188066090",
        // "6666000188065871",
        // "6666000188065668",
        // "6666000188065043",
        // "6666000188064831",
        // "6666000187846130",
        // "6666000187629184",
    };

    @Override
    protected String getTaskName() {
        return "V2MerchantBasicdataModify";
    }

    @Override
    protected MerConfig getMerConfig() {
        MerConfig merConfig = new MerConfig();
        merConfig.setProductId(HemaConfig.PRODUCT_ID);
        merConfig.setSysId(HemaConfig.SYS_ID);
        merConfig.setRsaPrivateKey(HemaConfig.RSA_PRIVATE_KEY);
        merConfig.setRsaPublicKey(HemaConfig.RSA_PUBLIC_KEY);
        return merConfig;
    }

    @Override
    protected String[] getStaticHuifuIds() {
        if (!settleCards.isEmpty()) {
            return settleCards.keySet().toArray(new String[0]);
        }

        if (!extMerIds.isEmpty()) {
            return extMerIds.keySet().toArray(new String[0]);
        }

        System.out.println(extMerIds);
        // return null;
        return HUIFU_IDS;
    }

    @Override
    protected TaskExecutionResult doExecute(String huifuId) throws Exception {
        V2MerchantBasicdataModifyRequest request = new V2MerchantBasicdataModifyRequest();
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        request.setUpperHuifuId(HemaConfig.SYS_ID);
        request.setHuifuId(huifuId);
        request.setExtendInfo(getExtendInfos(huifuId));

        String requestJson = JSONObject.toJSONString(request);
        log.info("{} Request: {}", huifuId, requestJson);

        Map<String, Object> response = BasePayClient.request(request, false);
        log.info("{} Response: {}", huifuId, JSONObject.toJSONString(response));

        return new TaskExecutionResult(requestJson, response);
    }

    private Map<String, Object> getExtendInfos(String huifuId) {
        Map<String, Object> extendInfoMap = new HashMap<>();
        extendInfoMap.put("settle_config", getSettleConfig());
        extendInfoMap.put("card_info",getCardInfo(huifuId));
        return extendInfoMap;
    }

    private String getSettleConfig(){
        JSONObject jo = new JSONObject();
        jo.put("settle_status","1");
        jo.put("settle_cycle","D1");
        jo.put("settle_abstract","汇付商编$$ID");

        return JSONObject.toJSONString(jo);
    }

    private String getCardInfo(String huifuId){
        CardInfo cardInfo = settleCards.get(huifuId);
        if (cardInfo == null) {
            throw new IllegalArgumentException("card_info not found for huifu_id: " + huifuId);
        }

        JSONObject jo = new JSONObject();
        jo.put("card_type",cardInfo.cardType);
        jo.put("card_name",cardInfo.cardName);
        jo.put("card_no",cardInfo.cardNo);
        jo.put("prov_id",cardInfo.provId);
        jo.put("area_id",cardInfo.areaId);
        jo.put("branch_code",cardInfo.branchCode);
    
        return JSONObject.toJSONString(jo);
    }

    private static Map<String, String> loadExtMerIds() {
        Map<String, String> result = new LinkedHashMap<>();
        if (inputExcelPath() == null || inputExcelPath().isBlank()) {
            return result;
        }

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

    private static Map<String, CardInfo> loadSettleCards() {
        Map<String, CardInfo> result = new LinkedHashMap<>();
        Path path = Path.of(settleCardExcelPath());
        if (!Files.exists(path)) {
            System.err.println("Settle card Excel not found: " + settleCardExcelPath());
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

                String huifuId = formatter.formatCellValue(row.getCell(COL_CARD_HUIFU_ID)).trim();
                if (huifuId.isEmpty()) {
                    continue;
                }

                result.put(huifuId, new CardInfo(
                        formatter.formatCellValue(row.getCell(COL_CARD_TYPE)).trim(),
                        formatter.formatCellValue(row.getCell(COL_CARD_NAME)).trim(),
                        formatter.formatCellValue(row.getCell(COL_CARD_NO)).trim(),
                        formatter.formatCellValue(row.getCell(COL_CARD_PROV_ID)).trim(),
                        formatter.formatCellValue(row.getCell(COL_CARD_AREA_ID)).trim(),
                        formatter.formatCellValue(row.getCell(COL_CARD_BRANCH_CODE)).trim()
                ));
            }
            System.out.println("Loaded settle card mappings: " + result.size());
        } catch (Exception e) {
            System.err.println("Read settle card Excel failed: " + e.getMessage());
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        new V2MerchantBasicdataModifyTask().execute(args);
    }

    private static final class CardInfo {
        private final String cardType;
        private final String cardName;
        private final String cardNo;
        private final String provId;
        private final String areaId;
        private final String branchCode;

        private CardInfo(String cardType, String cardName, String cardNo,
                         String provId, String areaId, String branchCode) {
            this.cardType = cardType;
            this.cardName = cardName;
            this.cardNo = cardNo;
            this.provId = provId;
            this.areaId = areaId;
            this.branchCode = branchCode;
        }
    }
}
