package com.offerwave.util;

import com.offerwave.dto.AdminJobUpsertDto;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobImportParserTest {

    @Test
    void shouldParseCsvByJobsTableHeaders() throws Exception {
        String csv = "\uFEFFcompany_name,company_type,job_title,city,recruit_type,salary_min,audit_status,unique_hash\n"
                + "OfferWave,互联网,Java工程师,上海,校招,25000,0,manual-hash\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        List<AdminJobUpsertDto> jobs = JobImportParser.parse(file);

        assertEquals(1, jobs.size());
        AdminJobUpsertDto job = jobs.get(0);
        assertEquals("OfferWave", job.getCompanyName());
        assertEquals("互联网", job.getCompanyType());
        assertEquals("Java工程师", job.getJobTitle());
        assertEquals("上海", job.getCity());
        assertEquals("校招", job.getRecruitType());
        assertEquals(25000, job.getSalaryMin());
        assertEquals(0, job.getAuditStatus());
    }

    @Test
    void shouldParseExcelByJobsTableHeaders() throws Exception {
        byte[] content;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("jobs");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("company_name");
            header.createCell(1).setCellValue("company_type");
            header.createCell(2).setCellValue("job_title");
            header.createCell(3).setCellValue("city");
            header.createCell(4).setCellValue("recruit_type");
            header.createCell(5).setCellValue("salary_max");

            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("OfferWave");
            row.createCell(1).setCellValue("互联网");
            row.createCell(2).setCellValue("后端开发");
            row.createCell(3).setCellValue("北京");
            row.createCell(4).setCellValue("实习");
            row.createCell(5).setCellValue(18000);

            workbook.write(outputStream);
            content = outputStream.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jobs.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content);

        List<AdminJobUpsertDto> jobs = JobImportParser.parse(file);

        assertEquals(1, jobs.size());
        AdminJobUpsertDto job = jobs.get(0);
        assertEquals("OfferWave", job.getCompanyName());
        assertEquals("互联网", job.getCompanyType());
        assertEquals("后端开发", job.getJobTitle());
        assertEquals("北京", job.getCity());
        assertEquals("实习", job.getRecruitType());
        assertEquals(18000, job.getSalaryMax());
    }
}
