package com.platform.task.controller;

import com.alibaba.fastjson.JSONObject;
import com.huifu.bspay.sdk.opps.client.BasePayClient;
import com.huifu.bspay.sdk.opps.core.BasePay;
import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.platform.task.controller.util.ExcelRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 抽象基类，封装了公共的任务执行逻辑，包括：
 * 1. SDK 初始化
 * 2. 多线程并发处理
 * 3. 网络异常自动重试
 * 4. Excel 结果记录
 */
public abstract class BaseController {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    // 线程池配置
    protected static final int THREAD_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
    protected static final int MAX_RETRY = 3;
    protected static final long EXECUTOR_AWAIT_SECONDS = 60L;

    private String excelPath;

    /**
     * 子类需提供任务名称，用于生成 Excel 文件名
     */
    protected abstract String getTaskName();

    /**
     * 子类需提供 SDK 配置信息
     */
    protected abstract MerConfig getMerConfig();

    /**
     * 子类需提供静态配置的汇付商户号列表（作为兜底）
     */
    protected abstract String[] getStaticHuifuIds();

    /**
     * 子类实现具体的业务接口调用逻辑
     */
    protected abstract TaskExecutionResult doExecute(String huifuId) throws Exception;

    /**
     * 任务执行入口
     */
    public void execute(String[] args) throws Exception {
        System.out.println("DEBUG: taskName property is: " + System.getProperty("taskName"));
        
        // 1. 初始化 SDK
        doInit(getMerConfig());

        // 2. 准备 Excel 记录文件
        this.excelPath = ExcelRecordService.getOrCreateExcelPath(getTaskName());
        System.out.println("Starting task: " + getTaskName() + ", Excel: " + excelPath);
        log.info("任务 [{}] 开始，Excel 记录文件: {}", getTaskName(), excelPath);

        // 3. 解析待处理的商户号
        List<String> huifuIds = ExcelRecordService.resolveHuifuIds(args, getStaticHuifuIds());
        if (huifuIds.isEmpty()) {
            log.warn("未配置待处理的汇付商户号，任务终止");
            return;
        }

        // 4. 并发执行
        int workerCount = Math.min(THREAD_POOL_SIZE, huifuIds.size());
        ExecutorService executorService = Executors.newFixedThreadPool(workerCount);
        CompletionService<InnerResult> cs = new ExecutorCompletionService<>(executorService);

        log.info("开始并发执行，商户总数={}，线程数={}", huifuIds.size(), workerCount);
        for (String huifuId : huifuIds) {
            cs.submit(() -> processMerchant(huifuId));
        }

        // 5. 收集结果
        int successCount = 0, failCount = 0;
        try {
            for (int i = 0; i < huifuIds.size(); i++) {
                InnerResult result = cs.take().get();
                if (result.success) {
                    successCount++;
                    log.info("{} 处理成功", result.huifuId);
                } else {
                    failCount++;
                    log.error("{} 处理最终失败: {}", result.huifuId, result.message);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            throw new RuntimeException("并发执行过程中发生不可恢复异常", e.getCause());
        } finally {
            shutdownExecutor(executorService);
        }

        log.info("任务 [{}] 执行完成，总数={}，成功={}，失败={}", getTaskName(), huifuIds.size(), successCount, failCount);
    }

    private void doInit(MerConfig merConfig) {
        try {
            BasePay.initWithMerConfig(merConfig);
            BasePay.debug = false;
        } catch (Exception e) {
            // TODO: handle exception
            log.error("初始化错误：{}", e.getMessage());
        }
    }

    private InnerResult processMerchant(String huifuId) {
        log.info("{} 开始处理", huifuId);
        int attempt = 0;
        while (true) {
            String requestTime = ExcelRecordService.nowStr();
            try {
                TaskExecutionResult result = doExecute(huifuId);
                String responseTime = ExcelRecordService.nowStr();

                // 记录成功结果
                String applyNo = extractApplyNo(result.response);
                ExcelRecordService.appendRecord(excelPath, huifuId, result.requestJson,
                        JSONObject.toJSONString(result.response), applyNo, requestTime, responseTime);

                return InnerResult.success(huifuId);
            } catch (IOException | InterruptedException e) {
                Throwable root = getRootCause(e);
                if (!isNetworkException(root)) {
                    log.error("{} 非网络异常，不重试: {}", huifuId, root.getMessage());
                    return recordError(huifuId, "非网络异常: " + root.getMessage(), requestTime);
                }
                attempt++;
                if (attempt > MAX_RETRY) {
                    log.error("{} 网络异常重试{}次后仍失败", huifuId, MAX_RETRY);
                    return recordError(huifuId, "网络异常重试后失败: " + root.getMessage(), requestTime);
                }
                long delay = (long) Math.pow(2, attempt) * 1000L;
                log.warn("{} 第{}次重试，延迟{}ms", huifuId, attempt, delay);
                sleepQuietly(delay);
            } catch (Exception e) {
                log.error("{} 调用发生业务异常: {}", huifuId, getRootCause(e).getMessage());
                return recordError(huifuId, getRootCause(e).getMessage(), requestTime);
            }
        }
    }

    private InnerResult recordError(String huifuId, String message, String requestTime) {
        ExcelRecordService.appendRecord(excelPath, huifuId, "", message, "", requestTime, ExcelRecordService.nowStr());
        return InnerResult.fail(huifuId, message);
    }

    private String extractApplyNo(Map<String, Object> response) {
        if (response == null)
            return "";
        Object applyNo = response.get("apply_no");
        if (applyNo == null) {
            Object data = response.get("data");
            if (data instanceof Map) {
                applyNo = ((Map<?, ?>) data).get("apply_no");
            }
        }
        return applyNo != null ? applyNo.toString() : "";
    }

    private Throwable getRootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t)
            t = t.getCause();
        return t;
    }

    private boolean isNetworkException(Throwable t) {
        return t instanceof SocketTimeoutException || t instanceof ConnectException
                || t instanceof UnknownHostException || t instanceof IOException;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void shutdownExecutor(ExecutorService es) {
        es.shutdown();
        try {
            if (!es.awaitTermination(EXECUTOR_AWAIT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("线程池超时关闭，未执行任务数={}", es.shutdownNow().size());
            }
        } catch (InterruptedException e) {
            es.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 业务执行结果封装
     */
    protected static class TaskExecutionResult {
        public String requestJson;
        public Map<String, Object> response;

        public TaskExecutionResult(String requestJson, Map<String, Object> response) {
            this.requestJson = requestJson;
            this.response = response;
        }
    }

    private static final class InnerResult {
        final String huifuId;
        final boolean success;
        final String message;

        private InnerResult(String h, boolean s, String m) {
            this.huifuId = h;
            this.success = s;
            this.message = m;
        }

        static InnerResult success(String h) {
            return new InnerResult(h, true, null);
        }

        static InnerResult fail(String h, String m) {
            return new InnerResult(h, false, m);
        }
    }
}
