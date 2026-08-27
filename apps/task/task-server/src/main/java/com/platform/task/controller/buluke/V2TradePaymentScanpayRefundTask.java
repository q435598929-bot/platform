package com.platform.task.controller.buluke;

import java.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.V2TradePaymentScanpayRefundRequest;
import com.platform.task.controller.util.TaskArguments;

/**
 * 扫码交易退款 - 示例
 *
 * @author sdk-generator
 * @Description
 */

public class V2TradePaymentScanpayRefundTask {

    public static void main(String[] args) throws Exception {

        // 1. 数据初始化
        doInit(getMerConfig());
        Map<String, String> pageInputs = TaskArguments.named(args);

        // 2.组装请求参数
        V2TradePaymentScanpayRefundRequest request = new V2TradePaymentScanpayRefundRequest();
        // 请求日期
        request.setReqDate(DateTools.getCurrentDateYYYYMMDD());
        // 请求流水号
        request.setReqSeqId(SequenceTools.getReqSeqId32());
        // 商户号
        request.setHuifuId(TaskArguments.value(pageInputs, "huifu_id", "6666000189266476"));
        // 申请退款金额
        request.setOrdAmt(TaskArguments.value(pageInputs, "ord_amt", "388.05"));
        // 原交易请求日期
        request.setOrgReqDate(TaskArguments.value(pageInputs, "org_req_date", "20260701"));

        // 设置非必填字段
        Map<String, Object> extendInfoMap = getExtendInfos(pageInputs);
        request.setExtendInfo(extendInfoMap);

        // 3. 发起API调用
        Map<String, Object> response = BasePayClient.request(request, false);
        System.out.println("返回数据:" + JSONObject.toJSONString(response));
    }


    private static MerConfig getMerConfig() {
        MerConfig merConfig = new MerConfig();
        merConfig.setProductId(BelukeConfig.PRODUCT_ID);
        merConfig.setSysId(BelukeConfig.SYS_ID);
        merConfig.setRsaPrivateKey(BelukeConfig.RSA_PRIVATE_KEY);
        merConfig.setRsaPublicKey(BelukeConfig.RSA_PUBLIC_KEY);
        return merConfig;
    }

    private static void doInit(MerConfig merConfig) throws Exception {
        BasePay.initWithMerConfig(merConfig);
        BasePay.debug = false;
    }

    /**
     * 非必填字段
     * @return
     */
    private static Map<String, Object> getExtendInfos(Map<String, String> pageInputs) {
        // 设置非必填字段
        Map<String, Object> extendInfoMap = new HashMap<>();
        // 原交易全局流水号
        extendInfoMap.put("org_hf_seq_id", TaskArguments.value(pageInputs, "org_hf_seq_id",
                "002900HS11A260701181825P0470a4f253b00000"));
        // 原交易微信支付宝的商户单号
        // extendInfoMap.put("org_party_order_id", "");
        // 原交易请求流水号
        // extendInfoMap.put("org_req_seq_id", "");
        // 分账对象
        // extendInfoMap.put("acct_split_bunch", getAcctSplitBunchRucan());
        // 聚合正扫微信拓展参数集合
        // extendInfoMap.put("wx_data", getWxData());
        // 数字货币扩展参数集合
        // extendInfoMap.put("digital_currency_data", getDigitalCurrencyData());
        // 补贴支付信息
        // extendInfoMap.put("combinedpay_data", getCombinedpayData());
        // 备注
        // extendInfoMap.put("remark", "");
        // 是否垫资退款
        // extendInfoMap.put("loan_flag", "");
        // 垫资承担者
        // extendInfoMap.put("loan_undertaker", "");
        // 垫资账户类型
        // extendInfoMap.put("loan_acct_type", "");
        // 安全信息
        // extendInfoMap.put("risk_check_data", getRiskCheckData());
        // 设备信息
        // extendInfoMap.put("terminal_device_data", getTerminalDeviceData());
        // 异步通知地址
        // extendInfoMap.put("notify_url", "");
        return extendInfoMap;
    }

    private static JSON getAcctInfosRucan() {
        JSONObject dto = new JSONObject();
        // 分账金额
        // dto.put("div_amt", "test");
        // 被分账方ID
        // dto.put("huifu_id", "test");
        // 垫资金额
        // dto.put("part_loan_amt", "");

        JSONArray dtoList = new JSONArray();
        dtoList.add(dto);
        return dtoList;
    }

    private static String getAcctSplitBunchRucan() {
        JSONObject dto = new JSONObject();
        // 分账信息列表
        // dto.put("acct_infos", getAcctInfosRucan());

        return dto.toJSONString();
    }

    private static JSON getGoodsDetail() {
        JSONObject dto = new JSONObject();
        // 商品编码
        // dto.put("goods_id", "test");
        // 优惠退款金额
        // dto.put("refund_amount", "test");
        // 商品退货数量
        // dto.put("refund_quantity", "test");
        // 商品单价
        // dto.put("price", "test");

        JSONArray dtoList = new JSONArray();
        dtoList.add(dto);
        return dtoList;
    }

    private static JSON getDetail() {
        JSONObject dto = new JSONObject();
        // 商品详情列表
        // dto.put("goods_detail", getGoodsDetail());

        return dto;
    }

    private static JSON getWxData() {
        JSONObject dto = new JSONObject();
        // 退款商品详情
        // dto.put("detail", getDetail());

        return dto;
    }

    private static String getDigitalCurrencyData() {
        JSONObject dto = new JSONObject();
        // 退款原因
        // dto.put("refund_desc", "");

        return dto.toJSONString();
    }

    private static String getCombinedpayData() {
        JSONObject dto = new JSONObject();
        // 补贴方汇付编号
        // dto.put("huifu_id", "test");
        // 补贴方类型
        // dto.put("user_type", "test");
        // 补贴方账户号
        // dto.put("acct_id", "test");
        // 补贴金额
        // dto.put("amount", "test");

        JSONArray dtoList = new JSONArray();
        dtoList.add(dto);
        return dtoList.toJSONString();
    }

    private static String getRiskCheckData() {
        JSONObject dto = new JSONObject();
        // ip地址
        // dto.put("ip_addr", "");
        // 基站地址
        // dto.put("base_station", "");
        // 纬度
        // dto.put("latitude", "");
        // 经度
        // dto.put("longitude", "");

        return dto.toJSONString();
    }

    private static String getTerminalDeviceData() {
        JSONObject dto = new JSONObject();
        // 设备类型
        // dto.put("device_type", "");
        // 交易设备IP
        // dto.put("device_ip", "");
        // 交易设备MAC
        // dto.put("device_mac", "");
        // 交易设备IMEI
        // dto.put("device_imei", "");
        // 交易设备IMSI
        // dto.put("device_imsi", "");
        // 交易设备ICCID
        // dto.put("device_icc_id", "");
        // 交易设备WIFIMAC
        // dto.put("device_wifi_mac", "");
        // 交易设备GPS
        // dto.put("device_gps", "");

        return dto.toJSONString();
    }

}
