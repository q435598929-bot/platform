package com.platform.task.controller.quhulian;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Direct POST task for modifying merchant MCC through the spin.cloudpnr endpoint.
 * The fixed request here is used for single-merchant verification before Excel batch mode.
 */
public class ModifyMerchantMccPostTask {

    private static final String URL = "https://spin.cloudpnr.com/api/cc/merchant/modifyMerchantMcc";
    private static final String DEFAULT_LOGIN_TYPE = "1";
    private static final String DEFAULT_MCC_CODE = "8299";
    private static final String DEFAULT_HUIFU_ID = "6666000158718563";
    private static final String DEFAULT_ORIGIN_SYSTEM = "ssp.operator";
    private static final String DEFAULT_PAY_WAY = "U";
    private static final String DEFAULT_PLATFORM_ID = "1";
    private static final String DEFAULT_PRODUCT_ID = "SPIN";
    private static final String DEFAULT_ROLE_TYPE = "1";
    private static final String DEFAULT_SYS_ID = "1";
    private static final String DEFAULT_USER_ID = "ronggui.liao_c";
    private static final String DEFAULT_USER_NAME = "\u5ed6\u8363\u8d35";
    private static final String DEFAULT_USER_TYPE = "3";
    private static final String HF_TOKEN_ENV = "HF_TOKEN";
    private static final String DEFAULT_HF_TOKEN = "eyJ0eXBlIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJqdGkiOiJyb25nZ3VpLmxpYW9fYyIsImlzcyI6IlNQSU4iLCJpYXQiOjE3ODMwNDIxNDQsImV4cCI6MTc4MzA3ODE0NCwicnQiOiIxIiwidW9pZCI6IjUiLCJjZiI6IiIsIm9zIjoic3NwLm9wZXJhdG9yIiwibHQiOiIxIiwic3VpZCI6Ik9QMDAyOVVURCIsInVpZCI6IkhGMDAxUktFMyIsInBkIjoiMSIsInJvcyI6Im9wZXJhdG9yIiwidW4iOiLlu5bojaPotLUiLCJsZyI6InJvbmdndWkubGlhb19jIiwidXQiOiIzIn0.vqHtGbrM-oanowFEScrx5oM3EjHBWeHuoVq9gYnXdJ5McW8YmKibbgsCcZ53d4SVENv4aqqOEcc1JkGdNvNvD_WQS42lD0a-QTbR_SoXJgdrRVKydaQ9rqbMOMN8dvI-99I6T3YVN4RUA5Mo6vT9p5D5H_-_qJ1FaCqHcAktLW7uLtFLbIZc2GKyvM18umq6xHvELNWSMMPTA8uMkoUVmvRqrVuvJQzftffwjeFQid1d_rN-zXLIQA8OtCxU5c-LZF2c-LUT-jFMmwkBFXNTVRYFxIZbvemIRcEZC42LhcR_6OL_Au4Jbj12XmJgjzCblyuNKsX6IjfH7YYGDjYHSw";

    public static void main(String[] args) throws Exception {
        new ModifyMerchantMccPostTask().execute(args);
    }

    public void execute(String[] args) throws Exception {
        RequestParams params = RequestParams.fromArgs(args);
        if (params.hfToken == null || params.hfToken.isBlank()) {
            throw new IllegalArgumentException("Missing hf_token. Set env HF_TOKEN or pass --hf_token=<token>.");
        }

        String requestJson = buildRequestBody(params);

        System.out.println("POST " + URL);
        System.out.println("Request: " + requestJson);
        System.out.println("hf_token loaded: true, length=" + params.hfToken.trim().length());

        HttpResponseText response = postJson(URL, requestJson, params.hfToken);

        System.out.println("HTTP Status: " + response.statusCode);
        System.out.println("Response: " + response.body);
        if (response.statusCode < 200 || response.statusCode >= 300) {
            throw new IllegalStateException("modifyMerchantMcc request failed, HTTP status=" + response.statusCode);
        }
    }

    private HttpResponseText postJson(String url, String requestJson, String hfToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("charset", "UTF-8");
        connection.setRequestProperty("format", "json");
        connection.setRequestProperty("version", "1.0");
        connection.setRequestProperty("Origin", "http://operation.hfinside.com");
        connection.setRequestProperty("Referer", "http://operation.hfinside.com/");
        if (hfToken != null && !hfToken.isBlank()) {
            connection.setRequestProperty("hf_token", hfToken.trim());
        }

        byte[] requestBytes = requestJson.getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("Content-Length", String.valueOf(requestBytes.length));
        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBytes);
        }

        int statusCode = connection.getResponseCode();
        InputStream responseStream = statusCode >= 200 && statusCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = "";
        if (responseStream != null) {
            try (InputStream is = responseStream) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        connection.disconnect();
        return new HttpResponseText(statusCode, body);
    }

    private String buildRequestBody(RequestParams params) {
        StringBuilder body = new StringBuilder();
        body.append('{');
        appendJsonField(body, "loginType", params.loginType);
        appendJsonField(body, "mcc_code", params.mccCode);
        appendJsonField(body, "huifu_id", params.huifuId);
        appendJsonField(body, "originSystem", params.originSystem);
        appendJsonField(body, "pay_way", params.payWay);
        appendJsonField(body, "platformId", params.platformId);
        appendJsonField(body, "product_id", params.productId);
        appendJsonField(body, "roleType", params.roleType);
        appendJsonField(body, "sys_id", params.sysId);
        appendJsonField(body, "userId", params.userId);
        appendJsonField(body, "userName", params.userName);
        appendJsonField(body, "userType", params.userType);
        body.setCharAt(body.length() - 1, '}');
        return body.toString();
    }

    private void appendJsonField(StringBuilder body, String key, String value) {
        body.append('"').append(escapeJson(key)).append("\":\"")
                .append(escapeJson(value))
                .append("\",");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static class RequestParams {
        private String loginType = DEFAULT_LOGIN_TYPE;
        private String mccCode = DEFAULT_MCC_CODE;
        private String huifuId = DEFAULT_HUIFU_ID;
        private String originSystem = DEFAULT_ORIGIN_SYSTEM;
        private String payWay = DEFAULT_PAY_WAY;
        private String platformId = DEFAULT_PLATFORM_ID;
        private String productId = DEFAULT_PRODUCT_ID;
        private String roleType = DEFAULT_ROLE_TYPE;
        private String sysId = DEFAULT_SYS_ID;
        private String userId = DEFAULT_USER_ID;
        private String userName = DEFAULT_USER_NAME;
        private String userType = DEFAULT_USER_TYPE;
        private String hfToken = firstNonBlank(System.getenv(HF_TOKEN_ENV), DEFAULT_HF_TOKEN);

        private static RequestParams fromArgs(String[] args) {
            RequestParams params = new RequestParams();
            if (args == null) {
                return params;
            }

            for (String arg : args) {
                if (arg == null || arg.isBlank()) {
                    continue;
                }
                applyKeyValue(params, arg);
            }
            return params;
        }

        private static void applyKeyValue(RequestParams params, String arg) {
            String normalized = arg.startsWith("--") ? arg.substring(2) : arg;
            int separatorIndex = normalized.indexOf('=');
            if (separatorIndex <= 0) {
                System.out.println("Ignored unsupported arg: " + arg);
                return;
            }

            String key = normalized.substring(0, separatorIndex).trim();
            String value = normalized.substring(separatorIndex + 1).trim();
            switch (key) {
                case "login_type", "loginType" -> params.loginType = value;
                case "mcc_code", "mccCode" -> params.mccCode = value;
                case "huifu_id", "huifuId" -> params.huifuId = value;
                case "origin_system", "originSystem" -> params.originSystem = value;
                case "pay_way", "payWay" -> params.payWay = value;
                case "platform_id", "platformId" -> params.platformId = value;
                case "product_id", "productId" -> params.productId = value;
                case "role_type", "roleType" -> params.roleType = value;
                case "sys_id", "sysId" -> params.sysId = value;
                case "user_id", "userId" -> params.userId = value;
                case "user_name", "userName" -> params.userName = value;
                case "user_type", "userType" -> params.userType = value;
                case "hf_token", "hfToken" -> params.hfToken = value;
                default -> System.out.println("Ignored unsupported arg: " + arg);
            }
        }

        private static String firstNonBlank(String first, String second) {
            if (first != null && !first.isBlank()) {
                return first.trim();
            }
            return second;
        }
    }

    private record HttpResponseText(int statusCode, String body) {
    }
}
