package com.offerwave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerwave.entity.ContentAuditLog;
import com.offerwave.entity.SensitiveWord;
import com.offerwave.mapper.ContentAuditLogMapper;
import com.offerwave.mapper.SensitiveWordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContentModerationServiceImpl implements ContentModerationService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private ContentAuditLogMapper contentAuditLogMapper;

    @Override
    public String sanitizeUserNote(Long userId, String originalNote) {
        if (!StringUtils.hasText(originalNote)) {
            return originalNote;
        }
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensitiveWord::getEnabled, 1);
        List<SensitiveWord> words = sensitiveWordMapper.selectList(wrapper);
        if (words.isEmpty()) {
            return originalNote;
        }

        String sanitized = originalNote;
        for (SensitiveWord item : words) {
            String word = item.getWord();
            if (!StringUtils.hasText(word)) {
                continue;
            }
            if (sanitized.contains(word)) {
                sanitized = sanitized.replace(word, "*".repeat(word.length()));
                ContentAuditLog log = new ContentAuditLog();
                log.setUserId(userId);
                log.setContentType("USER_NOTE");
                log.setContent(originalNote);
                log.setHitWord(word);
                log.setAction("MASKED");
                log.setCreatedAt(LocalDateTime.now());
                contentAuditLogMapper.insert(log);
            }
        }
        return sanitized;
    }

    @Override
    public Page<SensitiveWord> listSensitiveWords(Page<SensitiveWord> page) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SensitiveWord::getUpdatedAt);
        return sensitiveWordMapper.selectPage(page, wrapper);
    }

    @Override
    public SensitiveWord addSensitiveWord(String word, Integer enabled) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SensitiveWord::getWord, word);
        SensitiveWord existed = sensitiveWordMapper.selectOne(wrapper);
        if (existed != null) {
            existed.setEnabled(enabled == null ? 1 : enabled);
            existed.setUpdatedAt(LocalDateTime.now());
            sensitiveWordMapper.updateById(existed);
            return existed;
        }
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(word);
        entity.setEnabled(enabled == null ? 1 : enabled);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        sensitiveWordMapper.insert(entity);
        return entity;
    }

    @Override
    public boolean deleteSensitiveWord(Long id) {
        return sensitiveWordMapper.deleteById(id) > 0;
    }

    @Override
    public boolean updateSensitiveWordStatus(Long id, Integer enabled) {
        SensitiveWord entity = new SensitiveWord();
        entity.setId(id);
        entity.setEnabled(enabled == null ? 1 : enabled);
        entity.setUpdatedAt(LocalDateTime.now());
        return sensitiveWordMapper.updateById(entity) > 0;
    }

    @Override
    public Page<ContentAuditLog> listAuditLogs(Page<ContentAuditLog> page) {
        LambdaQueryWrapper<ContentAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ContentAuditLog::getCreatedAt);
        return contentAuditLogMapper.selectPage(page, wrapper);
    }
}
