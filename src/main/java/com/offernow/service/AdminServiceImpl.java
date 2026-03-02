package com.offernow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.dto.AdminJobUpsertDto;
import com.offernow.entity.CrawlerSyncError;
import com.offernow.entity.CrawlerSyncLog;
import com.offernow.entity.Job;
import com.offernow.entity.Membership;
import com.offernow.entity.SystemConfig;
import com.offernow.entity.User;
import com.offernow.mapper.CrawlerSyncErrorMapper;
import com.offernow.mapper.CrawlerSyncLogMapper;
import com.offernow.mapper.JobMapper;
import com.offernow.mapper.MembershipMapper;
import com.offernow.mapper.SystemConfigMapper;
import com.offernow.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Pattern DEADLINE_DATE_PATTERN = Pattern.compile("(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})");
    private static final DateTimeFormatter[] DEADLINE_DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd")
    };
    private static final DateTimeFormatter[] DEADLINE_DATETIME_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
    };

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private CrawlerSyncLogMapper crawlerSyncLogMapper;

    @Autowired
    private CrawlerSyncErrorMapper crawlerSyncErrorMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MembershipMapper membershipMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public Page<Job> listPendingAuditJobs(Page<Job> page) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getAuditStatus, 0).orderByAsc(Job::getUpdatedAt);
        return jobMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean batchAuditJobs(List<Long> jobIds, Integer auditStatus) {
        if (jobIds == null || jobIds.isEmpty()) {
            return false;
        }
        Job patch = new Job();
        patch.setAuditStatus(auditStatus);
        int updated = 0;
        for (Long jobId : jobIds) {
            patch.setId(jobId);
            updated += jobMapper.updateById(patch);
        }
        return updated > 0;
    }

    @Override
    public boolean batchDeleteJobs(List<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            return false;
        }
        return jobMapper.deleteBatchIds(jobIds) > 0;
    }

    @Override
    public Page<Job> listAllJobs(Page<Job> page, String keyword, String companyName, Integer auditStatus) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w.like(Job::getJobTitle, keyword).or().like(Job::getCompanyName, keyword));
        wrapper.like(StringUtils.hasText(companyName), Job::getCompanyName, companyName);
        wrapper.eq(auditStatus != null, Job::getAuditStatus, auditStatus);
        wrapper.orderByDesc(Job::getUpdatedAt);
        return jobMapper.selectPage(page, wrapper);
    }

    @Override
    public Job saveOrUpdateJob(AdminJobUpsertDto dto) {
        Job job = new Job();
        BeanUtils.copyProperties(dto, job);
        if (!StringUtils.hasText(job.getUniqueHash())
                && StringUtils.hasText(job.getCompanyName())
                && StringUtils.hasText(job.getJobTitle())
                && StringUtils.hasText(job.getCity())) {
            String raw = job.getCompanyName() + "_" + job.getJobTitle() + "_" + job.getCity();
            job.setUniqueHash(DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)));
        }
        if (job.getAuditStatus() == null) {
            job.setAuditStatus(1);
        }
        if (job.getId() == null) {
            jobMapper.insert(job);
        } else {
            jobMapper.updateById(job);
        }
        return job.getId() == null ? null : jobMapper.selectById(job.getId());
    }

    @Override
    @Transactional
    public int batchCreateJobs(List<AdminJobUpsertDto> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return 0;
        }
        int inserted = 0;
        for (AdminJobUpsertDto dto : jobs) {
            if (!StringUtils.hasText(dto.getCompanyName())
                    || !StringUtils.hasText(dto.getJobTitle())
                    || !StringUtils.hasText(dto.getCity())
                    || !StringUtils.hasText(dto.getCompanyType())
                    || !StringUtils.hasText(dto.getRecruitType())) {
                continue;
            }
            Job job = new Job();
            BeanUtils.copyProperties(dto, job);
            String raw = job.getCompanyName() + "_" + job.getJobTitle() + "_" + job.getCity();
            job.setUniqueHash(DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)));
            if (job.getAuditStatus() == null) {
                job.setAuditStatus(1);
            }
            inserted += jobMapper.insert(job);
        }
        return inserted;
    }

    @Override
    public int batchUpdateCompanyProcessStage(String companyName, String processStage) {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Job::getCompanyName, companyName);
        List<Job> jobs = jobMapper.selectList(wrapper);
        int updated = 0;
        for (Job job : jobs) {
            Job patch = new Job();
            patch.setId(job.getId());
            patch.setProcessStage(processStage);
            updated += jobMapper.updateById(patch);
        }
        return updated;
    }

    @Override
    public int cleanupExpiredJobs() {
        LambdaQueryWrapper<Job> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Job::getAuditStatus, 1);
        List<Job> onlineJobs = jobMapper.selectList(wrapper);
        List<Long> expiredIds = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Job job : onlineJobs) {
            Optional<LocalDate> deadlineOpt = parseDeadline(job.getDeadline());
            if (deadlineOpt.isPresent() && deadlineOpt.get().isBefore(today)) {
                expiredIds.add(job.getId());
            }
        }
        if (expiredIds.isEmpty()) {
            return 0;
        }
        int updated = 0;
        for (Long id : expiredIds) {
            Job patch = new Job();
            patch.setId(id);
            patch.setAuditStatus(2);
            updated += jobMapper.updateById(patch);
        }
        return updated;
    }

    private Optional<LocalDate> parseDeadline(String rawDeadline) {
        if (!StringUtils.hasText(rawDeadline)) {
            return Optional.empty();
        }
        String value = rawDeadline.trim();

        for (DateTimeFormatter formatter : DEADLINE_DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDate.parse(value, formatter));
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }

        for (DateTimeFormatter formatter : DEADLINE_DATETIME_FORMATTERS) {
            try {
                return Optional.of(LocalDateTime.parse(value, formatter).toLocalDate());
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }

        Matcher matcher = DEADLINE_DATE_PATTERN.matcher(value);
        if (matcher.find()) {
            String normalized = matcher.group(1).replace('/', '-').replace('.', '-');
            for (DateTimeFormatter formatter : DEADLINE_DATE_FORMATTERS) {
                try {
                    return Optional.of(LocalDate.parse(normalized, formatter));
                } catch (DateTimeParseException ignored) {
                    // try next pattern
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Page<CrawlerSyncLog> listCrawlerSyncLogs(Page<CrawlerSyncLog> page) {
        LambdaQueryWrapper<CrawlerSyncLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CrawlerSyncLog::getCreatedAt);
        return crawlerSyncLogMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<CrawlerSyncError> listCrawlerSyncErrors(Page<CrawlerSyncError> page) {
        LambdaQueryWrapper<CrawlerSyncError> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CrawlerSyncError::getCreatedAt);
        return crawlerSyncErrorMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<User> listUsers(Page<User> page, String keyword, Integer accountStatus) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword),
                w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        wrapper.eq(accountStatus != null, User::getAccountStatus, accountStatus);
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean updateUserStatus(Long userId, Integer accountStatus) {
        User patch = new User();
        patch.setId(userId);
        patch.setAccountStatus(accountStatus);
        return userMapper.updateById(patch) > 0;
    }

    @Override
    public boolean adjustUserBenefits(Long userId, Integer membershipId, Integer customTrackLimit) {
        User patch = new User();
        patch.setId(userId);
        if (membershipId != null) {
            patch.setMembershipId(membershipId);
            if (membershipId <= 1) {
                patch.setMembershipExpireAt(null);
            }
        }
        patch.setCustomTrackLimit(customTrackLimit);
        return userMapper.updateById(patch) > 0;
    }

    @Override
    public List<Membership> listMemberships() {
        LambdaQueryWrapper<Membership> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Membership::getId);
        return membershipMapper.selectList(wrapper);
    }

    @Override
    public boolean updateMembership(Membership membership) {
        return membershipMapper.updateById(membership) > 0;
    }

    @Override
    public Page<SystemConfig> listSystemConfigs(Page<SystemConfig> page, String configKey) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(configKey), SystemConfig::getConfigKey, configKey);
        wrapper.orderByDesc(SystemConfig::getUpdatedAt);
        return systemConfigMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean upsertSystemConfig(String key, String value, String description) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig existed = systemConfigMapper.selectOne(wrapper);
        if (existed == null) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            return systemConfigMapper.insert(config) > 0;
        }
        SystemConfig patch = new SystemConfig();
        patch.setId(existed.getId());
        patch.setConfigValue(value);
        patch.setDescription(description);
        return systemConfigMapper.updateById(patch) > 0;
    }
}
