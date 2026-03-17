package com.offernow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offernow.dto.AdminJobUpsertDto;
import com.offernow.entity.Job;
import com.offernow.mapper.JobMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void batchCreateJobsShouldGenerateUniqueHashAndSkipDuplicates() {
        AdminJobUpsertDto importedJob = buildJob("OfferWave", "Java工程师", "上海");
        importedJob.setAnnouncement("first");

        AdminJobUpsertDto duplicatedJob = buildJob("OfferWave", "Java工程师", "上海");
        duplicatedJob.setAnnouncement("latest");

        AdminJobUpsertDto existingJob = buildJob("Existing Co", "测试工程师", "北京");

        Job existing = new Job();
        existing.setUniqueHash(md5("Existing Co_测试工程师_北京"));

        when(jobMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));
        when(jobMapper.insert(any(Job.class))).thenReturn(1);

        int inserted = adminService.batchCreateJobs(List.of(importedJob, duplicatedJob, existingJob));

        assertEquals(1, inserted);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobMapper, times(1)).insert(jobCaptor.capture());

        Job savedJob = jobCaptor.getValue();
        assertEquals(md5("OfferWave_Java工程师_上海"), savedJob.getUniqueHash());
        assertEquals("latest", savedJob.getAnnouncement());
        assertEquals("人工导入", savedJob.getSourceOrigin());
        assertEquals(1, savedJob.getAuditStatus());
    }

    private AdminJobUpsertDto buildJob(String companyName, String jobTitle, String city) {
        AdminJobUpsertDto dto = new AdminJobUpsertDto();
        dto.setCompanyName(companyName);
        dto.setCompanyType("互联网");
        dto.setJobTitle(jobTitle);
        dto.setCity(city);
        dto.setRecruitType("校招");
        return dto;
    }

    private String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
