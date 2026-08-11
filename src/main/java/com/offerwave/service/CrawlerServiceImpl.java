package com.offerwave.service;

import com.offerwave.dto.CrawlerJobItemDto;
import com.offerwave.dto.CrawlerSyncDto;
import com.offerwave.entity.User;
import com.offerwave.util.JobUniqueHashGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates crawler validation, transactional job writes, and durable audit records.
 */
@Service
public class CrawlerServiceImpl implements CrawlerService {

    @Autowired
    private CrawlerJobBatchWriter jobBatchWriter;

    @Autowired
    private CrawlerSyncAuditService auditService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    @Override
    public Map<String, Integer> syncJobs(CrawlerSyncDto syncDto) {
        if (syncDto == null || syncDto.getItems() == null || syncDto.getItems().isEmpty()) {
            throw new IllegalArgumentException("items list must not be empty");
        }
        if (!StringUtils.hasText(syncDto.getBatchId())) {
            throw new IllegalArgumentException("batchId must not be blank");
        }
        if (syncDto.getBatchId().length() > 100) {
            throw new IllegalArgumentException("batchId must not exceed 100 characters");
        }

        int receivedCount = syncDto.getItems().size();
        int invalidCount = 0;
        Long operatorUserId = getCurrentUserId();

        try {
            List<CrawlerJobItemDto> validItems = new ArrayList<>();
            for (CrawlerJobItemDto item : syncDto.getItems()) {
                if (!isValid(item)) {
                    invalidCount++;
                    auditService.recordItemError(
                            syncDto.getBatchId(),
                            item == null ? null : item.getUniqueHash(),
                            String.valueOf(item),
                            "missing required fields");
                    continue;
                }

                // Client-provided hashes are advisory only. The backend owns the canonical identity.
                item.setUniqueHash(JobUniqueHashGenerator.generate(
                        item.getCompanyName(), item.getJobTitle(), item.getCity()));
                validItems.add(item);
            }

            CrawlerJobBatchWriter.Result writeResult = jobBatchWriter.write(validItems);
            auditService.recordSyncLog(
                    syncDto.getBatchId(),
                    receivedCount,
                    writeResult.insertedCount(),
                    writeResult.updatedCount(),
                    invalidCount,
                    true,
                    null,
                    operatorUserId);

            Map<String, Integer> result = new HashMap<>();
            result.put("received_count", receivedCount);
            result.put("inserted_count", writeResult.insertedCount());
            result.put("updated_count", writeResult.updatedCount());
            result.put("failed_count", invalidCount);
            return result;
        } catch (RuntimeException ex) {
            try {
                auditService.recordSyncLog(
                        syncDto.getBatchId(),
                        receivedCount,
                        0,
                        0,
                        receivedCount,
                        false,
                        ex.getMessage(),
                        operatorUserId);
            } catch (RuntimeException auditException) {
                ex.addSuppressed(auditException);
            }
            throw ex;
        }
    }

    private boolean isValid(CrawlerJobItemDto item) {
        return item != null
                && StringUtils.hasText(item.getCompanyName())
                && StringUtils.hasText(item.getCompanyType())
                && StringUtils.hasText(item.getJobTitle())
                && StringUtils.hasText(item.getCity())
                && StringUtils.hasText(item.getRecruitType());
    }
}
