package com.offerwave.service;

import com.offerwave.dto.CrawlerJobItemDto;
import com.offerwave.dto.CrawlerSyncDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrawlerServiceImplTest {

    @Mock
    private CrawlerJobBatchWriter jobBatchWriter;

    @Mock
    private CrawlerSyncAuditService auditService;

    @InjectMocks
    private CrawlerServiceImpl crawlerService;

    @Test
    void shouldRecomputeCanonicalHashInsteadOfTrustingClientHash() {
        CrawlerJobItemDto item = buildItem(" OfferWave ", "JAVA工程师", " 上海 ");
        item.setUniqueHash("client-controlled-hash");
        CrawlerSyncDto request = request(item);
        when(jobBatchWriter.write(any())).thenReturn(new CrawlerJobBatchWriter.Result(1, 0));

        crawlerService.syncJobs(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CrawlerJobItemDto>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jobBatchWriter).write(itemsCaptor.capture());
        assertEquals(md5("offerwave|java工程师|上海"), itemsCaptor.getValue().get(0).getUniqueHash());
    }

    @Test
    void shouldPersistFailureAuditAfterWriteTransactionFails() {
        CrawlerSyncDto request = request(buildItem("OfferWave", "Java工程师", "上海"));
        RuntimeException failure = new RuntimeException("write failed");
        when(jobBatchWriter.write(any())).thenThrow(failure);

        assertThrows(RuntimeException.class, () -> crawlerService.syncJobs(request));

        verify(auditService).recordSyncLog(
                eq("batch-1"), eq(1), eq(0), eq(0), eq(1), eq(false), eq("write failed"), eq(null));
    }

    @Test
    void auditAndWriteTransactionsShouldBeIndependent() throws Exception {
        Transactional orchestration = CrawlerServiceImpl.class
                .getMethod("syncJobs", CrawlerSyncDto.class)
                .getAnnotation(Transactional.class);
        Transactional writeTransaction = CrawlerJobBatchWriter.class
                .getMethod("write", List.class)
                .getAnnotation(Transactional.class);
        Transactional auditTransaction = CrawlerSyncAuditService.class
                .getMethod("recordSyncLog", String.class, int.class, int.class, int.class,
                        int.class, boolean.class, String.class, Long.class)
                .getAnnotation(Transactional.class);

        assertNull(orchestration);
        assertEquals(Propagation.REQUIRED, writeTransaction.propagation());
        assertEquals(Propagation.REQUIRES_NEW, auditTransaction.propagation());
    }

    private CrawlerSyncDto request(CrawlerJobItemDto item) {
        CrawlerSyncDto request = new CrawlerSyncDto();
        request.setBatchId("batch-1");
        request.setItems(List.of(item));
        return request;
    }

    private CrawlerJobItemDto buildItem(String companyName, String title, String city) {
        CrawlerJobItemDto item = new CrawlerJobItemDto();
        item.setCompanyName(companyName);
        item.setCompanyType("互联网");
        item.setJobTitle(title);
        item.setCity(city);
        item.setRecruitType("校招");
        return item;
    }

    private String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
