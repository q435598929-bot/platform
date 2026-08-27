package com.platform.task.controller.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Excel 记录服务 —— 线程安全
 *
 * 列布局（0-indexed）：
 *   0  huifu_id
 *   1  request_data
 *   2  response_data
 *   3  apply_no
 *   4  apply_result
 *   5  request_time
 *   6  response_time
 *   7  apply_query_time
 */
public class ExcelRecordService {

    // ==================== 常量 ====================
    private static final String OUTPUT_DIR   = "output";
    private static final String DATE_FMT     = "yyyyMMdd";
    private static final String DATETIME_FMT = "yyyy-MM-dd HH:mm:ss";

    // 列索引
    public static final int COL_HUIFU_ID         = 0;
    public static final int COL_REQUEST_DATA     = 1;
    public static final int COL_RESPONSE_DATA    = 2;
    public static final int COL_APPLY_NO         = 3;
    public static final int COL_APPLY_RESULT     = 4;
    public static final int COL_REQUEST_TIME     = 5;
    public static final int COL_RESPONSE_TIME    = 6;
    public static final int COL_APPLY_QUERY_TIME = 7;

    private static final String[] HEADERS = {
        "huifu_id", "request_data", "response_data", "apply_no", "apply_result",
        "request_time", "response_time", "apply_query_time"
    };

    // ==================== 锁管理（每个文件路径一把锁）====================
    private static final ConcurrentHashMap<String, ReentrantLock> FILE_LOCKS = new ConcurrentHashMap<>();

    private static ReentrantLock getLock(String filePath) {
        return FILE_LOCKS.computeIfAbsent(filePath, k -> new ReentrantLock(true));
    }

    // ==================== 公共 API ====================

    /**
     * 获取（或创建）当天的 Excel 文件路径，文件名格式: output/{taskName}_{YYYYMMDD}.xlsx
     */
    public static String getOrCreateExcelPath(String taskName) throws IOException {
        String today = DateTimeFormatter.ofPattern(DATE_FMT).format(LocalDateTime.now());
        Path dir = outputDirectory();
        Files.createDirectories(dir);
        String fileName = taskName + "_" + today + ".xlsx";
        return dir.resolve(fileName).toString();
    }

    /** 返回当前时间的格式化字符串，供调用方记录 request_time / response_time */
    public static String nowStr() {
        return DateTimeFormatter.ofPattern(DATETIME_FMT).format(LocalDateTime.now());
    }

    /**
     * 追加一条记录行（线程安全）
     *
     * @param excelPath    文件路径（由 getOrCreateExcelPath 生成）
     * @param huifuId      汇付商户号
     * @param requestJson  请求 JSON 字符串
     * @param responseJson 响应 JSON 字符串
     * @param applyNo      申请号（若响应中有则填入，否则传空串）
     * @param requestTime  发起请求的时间戳
     * @param responseTime 收到响应的时间戳
     */
    public static void appendRecord(String excelPath,
                                    String huifuId,
                                    String requestJson,
                                    String responseJson,
                                    String applyNo,
                                    String requestTime,
                                    String responseTime) {
        ReentrantLock lock = getLock(excelPath);
        lock.lock();
        try {
            Workbook workbook = openOrCreate(excelPath);
            Sheet sheet = getOrCreateSheet(workbook);

            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            setCellValue(row, COL_HUIFU_ID,         huifuId);
            setCellValue(row, COL_REQUEST_DATA,     requestJson);
            setCellValue(row, COL_RESPONSE_DATA,    responseJson);
            setCellValue(row, COL_APPLY_NO,         applyNo != null ? applyNo : "");
            setCellValue(row, COL_APPLY_RESULT,     "");
            setCellValue(row, COL_REQUEST_TIME,     requestTime != null ? requestTime : "");
            setCellValue(row, COL_RESPONSE_TIME,    responseTime != null ? responseTime : "");
            setCellValue(row, COL_APPLY_QUERY_TIME, "");

            saveWorkbook(workbook, excelPath);
        } catch (Exception e) {
            System.err.println("[ExcelRecordService] appendRecord error for " + huifuId + ": " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 读取所有 apply_no 不为空 且 apply_result 为空 的待查询行
     */
    public static List<ExcelRow> readPendingRows(String excelPath) {
        List<ExcelRow> result = new ArrayList<>();
        if (!Files.exists(Paths.get(excelPath))) {
            System.err.println("[ExcelRecordService] 文件不存在: " + excelPath);
            return result;
        }

        ReentrantLock lock = getLock(excelPath);
        lock.lock();
        try (InputStream is = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {  // 跳过表头行(0)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String applyNo     = getCellString(row, COL_APPLY_NO);
                String applyResult = getCellString(row, COL_APPLY_RESULT);

                if (applyNo != null && !applyNo.isBlank() && (applyResult == null || applyResult.isBlank())) {
                    ExcelRow er = new ExcelRow();
                    er.rowIndex      = i;
                    er.huifuId       = getCellString(row, COL_HUIFU_ID);
                    er.requestData   = getCellString(row, COL_REQUEST_DATA);
                    er.responseData  = getCellString(row, COL_RESPONSE_DATA);
                    er.applyNo       = applyNo;
                    er.applyResult   = applyResult;
                    er.requestTime   = getCellString(row, COL_REQUEST_TIME);
                    er.responseTime  = getCellString(row, COL_RESPONSE_TIME);
                    er.applyQueryTime = getCellString(row, COL_APPLY_QUERY_TIME);
                    result.add(er);
                }
            }
        } catch (Exception e) {
            System.err.println("[ExcelRecordService] readPendingRows error: " + e.getMessage());
        } finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * 读取全部数据行
     */
    public static List<ExcelRow> readAllRows(String excelPath) {
        List<ExcelRow> result = new ArrayList<>();
        if (!Files.exists(Paths.get(excelPath))) {
            System.err.println("[ExcelRecordService] 文件不存在: " + excelPath);
            return result;
        }

        ReentrantLock lock = getLock(excelPath);
        lock.lock();
        try (InputStream is = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                ExcelRow er = new ExcelRow();
                er.rowIndex      = i;
                er.huifuId       = getCellString(row, COL_HUIFU_ID);
                er.requestData   = getCellString(row, COL_REQUEST_DATA);
                er.responseData  = getCellString(row, COL_RESPONSE_DATA);
                er.applyNo       = getCellString(row, COL_APPLY_NO);
                er.applyResult   = getCellString(row, COL_APPLY_RESULT);
                er.requestTime   = getCellString(row, COL_REQUEST_TIME);
                er.responseTime  = getCellString(row, COL_RESPONSE_TIME);
                er.applyQueryTime = getCellString(row, COL_APPLY_QUERY_TIME);
                result.add(er);
            }
        } catch (Exception e) {
            System.err.println("[ExcelRecordService] readAllRows error: " + e.getMessage());
        } finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * 根据 apply_no 找到对应行，将 apply_result 写入（线程安全）
     */
    public static void updateApplyResult(String excelPath, String applyNo, String applyResult) {
        if (applyNo == null || applyNo.isBlank()) return;

        ReentrantLock lock = getLock(excelPath);
        lock.lock();
        try {
            Workbook workbook = openOrCreate(excelPath);
            Sheet sheet = workbook.getSheetAt(0);
            boolean updated = false;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String rowApplyNo = getCellString(row, COL_APPLY_NO);
                if (applyNo.equals(rowApplyNo)) {
                    setCellValue(row, COL_APPLY_RESULT,     applyResult != null ? applyResult : "");
                    setCellValue(row, COL_APPLY_QUERY_TIME, nowStr());
                    updated = true;
                }
            }

            if (updated) {
                saveWorkbook(workbook, excelPath);
            } else {
                workbook.close();
                System.err.println("[ExcelRecordService] apply_no 未找到: " + applyNo);
            }
        } catch (Exception e) {
            System.err.println("[ExcelRecordService] updateApplyResult error apply_no=" + applyNo + ": " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resolves huifu_ids using priority: CLI args > Excel input file (--excel=<path>) > static array.
     *
     * <p>Usage examples:
     * <ul>
     *   <li>Manual:  {@code java Task 6666000100000001 6666000100000002}</li>
     *   <li>Excel:   {@code java Task --excel=input/merchants.xlsx}</li>
     *   <li>Static:  no args — falls back to {@code staticIds[]}</li>
     * </ul>
     *
     * @param args      command-line arguments passed to main()
     * @param staticIds hardcoded fallback array defined in each task class
     */
    public static List<String> resolveHuifuIds(String[] args, String[] staticIds) {
        Set<String> ids = new LinkedHashSet<>();
        String excelPath = null;

        if (args != null) {
            for (String arg : args) {
                if (arg == null) continue;
                if (arg.startsWith("--excel=")) {
                    excelPath = arg.substring("--excel=".length()).trim();
                } else if (!arg.isBlank()) {
                    ids.add(arg.trim());
                }
            }
        }

        if (ids.isEmpty() && excelPath != null) {
            ids.addAll(readHuifuIdsFromExcel(excelPath));
        }

        if (ids.isEmpty() && staticIds != null) {
            Arrays.stream(staticIds)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(ids::add);
        }

        return new ArrayList<>(ids);
    }

    /**
     * Reads huifu_ids from the first column of an Excel file, skipping the header row.
     * Supports both plain ID lists and output files produced by this service.
     */
    public static List<String> readHuifuIdsFromExcel(String excelPath) {
        List<String> ids = new ArrayList<>();
        if (!Files.exists(Paths.get(excelPath))) {
            System.err.println("[ExcelRecordService] 输入文件不存在: " + excelPath);
            return ids;
        }
        try (InputStream is = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {  // skip header row(0)
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String id = getCellString(row, COL_HUIFU_ID);
                if (id != null && !id.isBlank()) {
                    ids.add(id.trim());
                }
            }
        } catch (Exception e) {
            System.err.println("[ExcelRecordService] readHuifuIdsFromExcel error: " + e.getMessage());
        }
        return ids;
    }

    /**
     * 扫描 output 目录下所有 xlsx 文件，返回含有待查询行的文件路径列表
     */
    public static List<String> findExcelFilesWithPendingRows() {
        List<String> result = new ArrayList<>();
        Path dir = outputDirectory();
        if (!Files.exists(dir)) return result;
        try {
            Files.list(dir)
                    .filter(p -> p.toString().endsWith(".xlsx"))
                    .forEach(p -> {
                        List<ExcelRow> pending = readPendingRows(p.toString());
                        if (!pending.isEmpty()) result.add(p.toString());
                    });
        } catch (IOException e) {
            System.err.println("[ExcelRecordService] findExcelFilesWithPendingRows error: " + e.getMessage());
        }
        return result;
    }

    // ==================== 内部工具 ====================

    private static Path outputDirectory() {
        return Paths.get(TaskPathResolver.value("TASK_OUTPUT_DIR", OUTPUT_DIR));
    }

    private static Workbook openOrCreate(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                return new XSSFWorkbook(is);
            }
        }
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("records");
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }
        return workbook;
    }

    private static Sheet getOrCreateSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            Sheet sheet = workbook.createSheet("records");
            Row header  = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            return sheet;
        }
        return workbook.getSheetAt(0);
    }

    private static void saveWorkbook(Workbook workbook, String filePath) throws IOException {
        try (OutputStream os = new FileOutputStream(filePath)) {
            workbook.write(os);
        }
        workbook.close();
    }

    private static void setCellValue(Row row, int colIdx, String value) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(value != null ? value : "");
    }

    private static String getCellString(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    // ==================== 数据模型 ====================

    public static class ExcelRow {
        public int    rowIndex;
        public String huifuId;
        public String requestData;
        public String responseData;
        public String applyNo;
        public String applyResult;
        public String requestTime;
        public String responseTime;
        public String applyQueryTime;

        @Override
        public String toString() {
            return "ExcelRow{rowIndex=" + rowIndex + ", huifuId='" + huifuId
                    + "', applyNo='" + applyNo + "', applyResult='" + applyResult + "'}";
        }
    }
}
