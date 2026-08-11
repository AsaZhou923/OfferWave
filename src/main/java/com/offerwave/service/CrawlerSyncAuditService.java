package com.offerwave.service;

import com.offerwave.entity.CrawlerSyncError;
import com.offerwave.entity.CrawlerSyncLog;
import com.offerwave.mapper.CrawlerSyncErrorMapper;
import com.offerwave.mapper.CrawlerSyncLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persists crawler audit evidence independently from the job-write transaction.
 */
@Service
public class CrawlerSyncAuditService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final int MAX_UNIQUE_HASH_LENGTH = 32;

    @Autowired
    private CrawlerSyncLogMapper crawlerSyncLogMapper;

    @Autowired
    private CrawlerSyncErrorMapper crawlerSyncErrorMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordItemError(String batchId, String uniqueHash, String payload, String message) {
        CrawlerSyncError error = new CrawlerSyncError();
        error.setBatchId(batchId);
        error.setUniqueHash(truncate(uniqueHash, MAX_UNIQUE_HASH_LENGTH));
        error.setPayload(payload);
        error.setErrorMessage(truncate(message, MAX_ERROR_MESSAGE_LENGTH));
        error.setCreatedAt(LocalDateTime.now());
        crawlerSyncErrorMapper.insert(error);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSyncLog(String batchId,
                              int receivedCount,
                              int insertedCount,
                              int updatedCount,
                              int failedCount,
                              boolean successful,
                              String errorMessage,
                              Long operatorUserId) {
        CrawlerSyncLog syncLog = new CrawlerSyncLog();
        syncLog.setBatchId(batchId);
        syncLog.setReceivedCount(receivedCount);
        syncLog.setInsertedCount(insertedCount);
        syncLog.setUpdatedCount(updatedCount);
        syncLog.setFailedCount(failedCount);
        syncLog.setStatus(successful ? 1 : 0);
        syncLog.setErrorMessage(truncate(errorMessage, MAX_ERROR_MESSAGE_LENGTH));
        syncLog.setOperatorUserId(operatorUserId);
        syncLog.setCreatedAt(LocalDateTime.now());
        crawlerSyncLogMapper.insert(syncLog);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
