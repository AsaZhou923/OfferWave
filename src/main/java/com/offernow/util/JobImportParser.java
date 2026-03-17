package com.offernow.util;

import com.offernow.dto.AdminJobUpsertDto;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JobImportParser {

    private static final Map<String, String> COLUMN_TO_PROPERTY = buildColumnToPropertyMap();
    private static final Set<String> INTEGER_PROPERTIES = Set.of("salaryMin", "salaryMax", "auditStatus");
    private static final Set<String> IGNORED_COLUMNS = Set.of(
            "id", "unique_hash", "uniquehash", "hash", "created_at", "createdat", "updated_at", "updatedat");

    private JobImportParser() {
    }

    public static List<AdminJobUpsertDto> parse(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim().toLowerCase();
        if (fileName.endsWith(".csv")) {
            try (InputStream inputStream = file.getInputStream()) {
                return parseCsv(inputStream);
            }
        }
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            try (InputStream inputStream = file.getInputStream()) {
                return parseExcel(inputStream);
            }
        }
        throw new IllegalArgumentException("仅支持 .xlsx、.xls 或 .csv 文件");
    }

    static List<AdminJobUpsertDto> parseExcel(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("导入文件没有可读取的工作表");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null || headerRow.getLastCellNum() < 0) {
                throw new IllegalArgumentException("导入文件缺少表头");
            }

            DataFormatter formatter = new DataFormatter();
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                headers.add(formatter.formatCellValue(headerRow.getCell(i)));
            }

            List<AdminJobUpsertDto> jobs = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> rowValues = new HashMap<>();
                boolean hasValue = false;
                for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                    String header = headers.get(columnIndex);
                    String value = formatter.formatCellValue(row.getCell(columnIndex));
                    if (StringUtils.hasText(value)) {
                        hasValue = true;
                    }
                    rowValues.put(header, value);
                }
                if (hasValue) {
                    jobs.add(mapRow(rowValues));
                }
            }
            return jobs;
        }
    }

    static List<AdminJobUpsertDto> parseCsv(InputStream inputStream) throws IOException {
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        List<List<String>> rows = parseCsvContent(stripBom(content));
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("导入文件缺少表头");
        }

        List<String> headers = rows.get(0);
        List<AdminJobUpsertDto> jobs = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (!hasAnyText(row)) {
                continue;
            }
            Map<String, String> rowValues = new HashMap<>();
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                String value = columnIndex < row.size() ? row.get(columnIndex) : "";
                rowValues.put(headers.get(columnIndex), value);
            }
            jobs.add(mapRow(rowValues));
        }
        return jobs;
    }

    private static AdminJobUpsertDto mapRow(Map<String, String> rowValues) {
        AdminJobUpsertDto dto = new AdminJobUpsertDto();
        BeanWrapper beanWrapper = new BeanWrapperImpl(dto);
        rowValues.forEach((header, value) -> applyValue(beanWrapper, header, value));
        return dto;
    }

    private static void applyValue(BeanWrapper beanWrapper, String header, String rawValue) {
        String normalizedHeader = normalizeHeader(header);
        if (!StringUtils.hasText(normalizedHeader) || IGNORED_COLUMNS.contains(normalizedHeader)) {
            return;
        }
        String property = COLUMN_TO_PROPERTY.get(normalizedHeader);
        if (property == null) {
            return;
        }

        String value = rawValue == null ? null : rawValue.trim();
        if (!StringUtils.hasText(value)) {
            return;
        }

        try {
            if (INTEGER_PROPERTIES.contains(property)) {
                beanWrapper.setPropertyValue(property, Integer.valueOf(value));
                return;
            }
            beanWrapper.setPropertyValue(property, value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("字段 " + header + " 的值不是合法整数: " + value);
        }
    }

    private static List<List<String>> parseCsvContent(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (current == '"') {
                if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    currentValue.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (current == ',' && !inQuotes) {
                currentRow.add(currentValue.toString());
                currentValue.setLength(0);
                continue;
            }
            if ((current == '\n' || current == '\r') && !inQuotes) {
                if (current == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                currentRow.add(currentValue.toString());
                currentValue.setLength(0);
                if (hasAnyText(currentRow)) {
                    rows.add(currentRow);
                }
                currentRow = new ArrayList<>();
                continue;
            }
            currentValue.append(current);
        }

        if (inQuotes) {
            throw new IllegalArgumentException("CSV 格式错误：存在未闭合的引号");
        }

        currentRow.add(currentValue.toString());
        if (hasAnyText(currentRow)) {
            rows.add(currentRow);
        }
        return rows;
    }

    private static boolean hasAnyText(List<String> values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return true;
            }
        }
        return false;
    }

    private static String stripBom(String value) {
        if (value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private static String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return stripBom(header).trim().replace("-", "_").replace(" ", "").toLowerCase();
    }

    private static Map<String, String> buildColumnToPropertyMap() {
        Map<String, String> mappings = new HashMap<>();
        register(mappings, "companyName", "company_name");
        register(mappings, "companyType", "company_type");
        register(mappings, "companyBusiness", "company_business");
        register(mappings, "jobTitle", "job_title");
        register(mappings, "city", "city");
        register(mappings, "recruitType", "recruit_type");
        register(mappings, "targetAudience", "target_audience");
        register(mappings, "announcement", "announcement");
        register(mappings, "salaryRange", "salary_range");
        register(mappings, "salaryMin", "salary_min");
        register(mappings, "salaryMax", "salary_max");
        register(mappings, "education", "education");
        register(mappings, "applyLink", "apply_link");
        register(mappings, "testInfo", "test_info");
        register(mappings, "processStage", "process_stage");
        register(mappings, "deadline", "deadline");
        register(mappings, "sourceOrigin", "source_origin");
        register(mappings, "auditStatus", "audit_status");
        return mappings;
    }

    private static void register(Map<String, String> mappings, String propertyName, String columnName) {
        mappings.put(normalizeHeader(propertyName), propertyName);
        mappings.put(normalizeHeader(columnName), propertyName);
    }
}
