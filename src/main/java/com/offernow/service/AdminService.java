package com.offernow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offernow.dto.AdminJobUpsertDto;
import com.offernow.entity.CrawlerSyncError;
import com.offernow.entity.CrawlerSyncLog;
import com.offernow.entity.Job;
import com.offernow.entity.Membership;
import com.offernow.entity.SystemConfig;
import com.offernow.entity.User;

import java.util.List;

public interface AdminService {

    Page<Job> listPendingAuditJobs(Page<Job> page);

    boolean batchAuditJobs(List<Long> jobIds, Integer auditStatus);

    boolean batchDeleteJobs(List<Long> jobIds);

    Page<Job> listAllJobs(Page<Job> page, String keyword, String companyName, Integer auditStatus);

    Job saveOrUpdateJob(AdminJobUpsertDto dto);

    int batchCreateJobs(List<AdminJobUpsertDto> jobs);

    int batchUpdateCompanyProcessStage(String companyName, String processStage);

    int cleanupExpiredJobs();

    Page<CrawlerSyncLog> listCrawlerSyncLogs(Page<CrawlerSyncLog> page);

    Page<CrawlerSyncError> listCrawlerSyncErrors(Page<CrawlerSyncError> page);

    Page<User> listUsers(Page<User> page, String keyword, Integer accountStatus);

    boolean updateUserStatus(Long userId, Integer accountStatus);

    boolean adjustUserBenefits(Long userId, Integer membershipId, Integer customTrackLimit);

    List<Membership> listMemberships();

    boolean updateMembership(Membership membership);

    Page<SystemConfig> listSystemConfigs(Page<SystemConfig> page, String configKey);

    boolean upsertSystemConfig(String key, String value, String description);
}
