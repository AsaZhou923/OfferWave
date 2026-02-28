package com.offernow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.entity.ContentAuditLog;
import com.offernow.entity.SensitiveWord;

public interface ContentModerationService {

    String sanitizeUserNote(Long userId, String originalNote);

    Page<SensitiveWord> listSensitiveWords(Page<SensitiveWord> page);

    SensitiveWord addSensitiveWord(String word, Integer enabled);

    boolean deleteSensitiveWord(Long id);

    boolean updateSensitiveWordStatus(Long id, Integer enabled);

    Page<ContentAuditLog> listAuditLogs(Page<ContentAuditLog> page);
}
