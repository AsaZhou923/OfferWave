package com.offerwave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerwave.dto.CrawlerJobItemDto;
import com.offerwave.entity.Job;
import com.offerwave.mapper.JobMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrawlerJobBatchWriterTest {

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private CrawlerJobBatchWriter writer;

    @Test
    void shouldPersistServerGeneratedHashOnInsert() {
        CrawlerJobItemDto item = new CrawlerJobItemDto();
        item.setCompanyName("OfferWave");
        item.setCompanyType("互联网");
        item.setJobTitle("Java工程师");
        item.setCity("上海");
        item.setRecruitType("校招");
        item.setUniqueHash("0123456789abcdef0123456789abcdef");

        when(jobMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(jobMapper.insert(any(Job.class))).thenReturn(1);

        CrawlerJobBatchWriter.Result result = writer.write(List.of(item));

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobMapper).insert(jobCaptor.capture());
        assertEquals(item.getUniqueHash(), jobCaptor.getValue().getUniqueHash());
        assertEquals(0, jobCaptor.getValue().getAuditStatus());
        assertEquals(1, result.insertedCount());
    }
}
