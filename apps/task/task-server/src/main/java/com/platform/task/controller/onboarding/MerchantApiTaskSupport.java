package com.platform.task.controller.onboarding;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.huifu.bspay.sdk.opps.core.request.BaseRequest;
import com.huifu.bspay.sdk.opps.core.utils.DateTools;
import com.huifu.bspay.sdk.opps.core.utils.SequenceTools;
import com.platform.task.controller.util.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MerchantApiTaskSupport {
    private static final Logger log = LoggerFactory.getLogger(MerchantApiTaskSupport.class);

    private MerchantApiTaskSupport() {}

    static Map<String, Object> request(BaseRequest request) throws Exception {
        initialize();
        populate(request);
        log.info("Merchant onboarding request type={}", request.getClass().getSimpleName());
        Map<String, Object> response = BasePayClient.request(request);
        TaskExecutionContext.recordOutput(response);
        log.info("Merchant onboarding response={}", JSON.toJSONString(response));
        return response;
    }

    static Map<String, Object> upload(BaseRequest request) throws Exception {
        initialize();
        populate(request);
        String filePath = value("file_path"), fileUrl = value("file_url");
        if ((filePath == null || filePath.isBlank()) == (fileUrl == null || fileUrl.isBlank())) {
            throw new IllegalArgumentException("Local image and file_url must choose exactly one");
        }
        Path temporary = null;
        try {
            File file;
            if (fileUrl != null && !fileUrl.isBlank()) {
                temporary = downloadPublicImage(fileUrl.trim());
                file = temporary.toFile();
            } else {
                file = new File(filePath.trim());
                if (!file.isFile()) throw new IllegalArgumentException("Upload file does not exist: " + filePath);
            }
            log.info("Merchant onboarding upload file={} type={}", file.getName(), value("file_type"));
            Map<String, Object> response = BasePayClient.upload(request, file);
            TaskExecutionContext.recordOutput(normalizeUploadOutput(response));
            log.info("Merchant onboarding upload response={}", JSON.toJSONString(response));
            return response;
        } finally {
            if (temporary != null) Files.deleteIfExists(temporary);
        }
    }

    static Map<String, Object> normalizeUploadOutput(Map<String, Object> response) {
        Map<String, Object> output = new LinkedHashMap<>(response == null ? Map.of() : response);
        String fileId = findNestedString(response, "file_id", 0);
        if (fileId != null && !fileId.isBlank()) output.put("file_id", fileId);
        return output;
    }

    private static String findNestedString(Object value, String key, int depth) {
        if (value == null || depth > 6) return null;
        if (value instanceof Map<?, ?> map) {
            Object direct = map.get(key);
            if (direct != null && !direct.toString().isBlank()) return direct.toString();
            for (Object nested : map.values()) {
                String found = findNestedString(nested, key, depth + 1);
                if (found != null) return found;
            }
        } else if (value instanceof List<?> list) {
            for (Object nested : list) {
                String found = findNestedString(nested, key, depth + 1);
                if (found != null) return found;
            }
        } else if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                try { return findNestedString(JSON.parse(trimmed), key, depth + 1); }
                catch (RuntimeException ignored) { return null; }
            }
        }
        return null;
    }

    private static Path downloadPublicImage(String source) throws Exception {
        URI uri = URI.create(source);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) throw new IllegalArgumentException("file_url only supports HTTP/HTTPS");
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("file_url must point to a public network address");
            }
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(15_000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "image/jpeg,image/png");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) throw new IllegalArgumentException("file_url download failed: HTTP " + status);
        String contentType = connection.getContentType();
        if (contentType == null || !(contentType.startsWith("image/jpeg") || contentType.startsWith("image/png"))) {
            throw new IllegalArgumentException("file_url must return a JPEG or PNG image");
        }
        long declaredSize = connection.getContentLengthLong();
        if (declaredSize > 10 * 1024 * 1024L) throw new IllegalArgumentException("Remote image exceeds 10 MB");
        Path target = Files.createTempFile("task-remote-image-", contentType.startsWith("image/png") ? ".png" : ".jpg");
        try (InputStream input = connection.getInputStream(); var output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192]; long total = 0; int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > 10 * 1024 * 1024L) throw new IllegalArgumentException("Remote image exceeds 10 MB");
                output.write(buffer, 0, read);
            }
        } catch (Exception error) {
            Files.deleteIfExists(target);
            throw error;
        } finally {
            connection.disconnect();
        }
        return target;
    }

    private static void initialize() throws Exception {
        MerConfig config = new MerConfig();
        config.setProductId(required("product_id"));
        config.setSysId(required("sys_id"));
        config.setRsaPrivateKey(requiredConfiguration("merchant_private_key", "rsa_private_key"));
        config.setRsaPublicKey(requiredConfiguration("huifu_public_key", "rsa_public_key"));
        BasePay.initWithMerConfig(config);
        BasePay.debug = false;
    }

    private static void populate(BaseRequest request) throws IllegalAccessException {
        for (Field field : request.getClass().getDeclaredFields()) {
            if (field.getType() != String.class) continue;
            JSONField jsonField = field.getAnnotation(JSONField.class);
            if (jsonField == null) continue;
            String key = jsonField.name();
            String fieldValue = switch (key) {
                case "req_seq_id" -> defaultValue(value(key), SequenceTools.getReqSeqId32());
                case "req_date" -> defaultValue(value(key), DateTools.getCurrentDateYYYYMMDD());
                default -> value(key);
            };
            if (fieldValue == null || fieldValue.isBlank()) continue;
            field.setAccessible(true);
            field.set(request, fieldValue.trim());
        }
    }

    private static String value(String key) {
        String direct = TaskExecutionContext.value(key);
        return direct == null ? TaskExecutionContext.value("merchant." + key) : direct;
    }

    private static String required(String key) {
        String result = value(key);
        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException("Merchant configuration is missing: " + key);
        }
        return result.trim();
    }

    static String requiredConfiguration(String canonicalKey, String... legacyKeys) {
        String result = value(canonicalKey);
        if (result == null || result.isBlank()) {
            for (String legacyKey : legacyKeys) {
                result = value(legacyKey);
                if (result != null && !result.isBlank()) break;
            }
        }
        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException("Merchant configuration is missing: " + canonicalKey);
        }
        return result.trim();
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
