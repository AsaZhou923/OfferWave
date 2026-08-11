package com.offerwave.config;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerwave.entity.SystemConfig;
import com.offerwave.entity.User;
import com.offerwave.mapper.SystemConfigMapper;
import com.offerwave.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Creates the first administrator once from environment-backed configuration.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String AUDIT_KEY = "bootstrap.admin.environment.v1";
    private static final int MIN_PASSWORD_LENGTH = 12;

    @Autowired
    private Environment environment;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = property("offerwave.bootstrap.admin.username");
        String email = property("offerwave.bootstrap.admin.email");
        String password = environment.getProperty("offerwave.bootstrap.admin.password");

        if (!StringUtils.hasText(username) && !StringUtils.hasText(email) && !StringUtils.hasText(password)) {
            return;
        }
        validate(username, email, password);

        SystemConfig existingAudit = systemConfigMapper.selectOne(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, AUDIT_KEY));
        if (existingAudit != null) {
            LOGGER.info("Administrator bootstrap already consumed; no account changes were made");
            return;
        }

        long existingAdminCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, 1));
        if (existingAdminCount > 0) {
            recordAudit(username, "skipped_existing_admin");
            LOGGER.info("Administrator bootstrap skipped because an administrator already exists");
            return;
        }
        if (userMapper.selectByUsername(username) != null) {
            throw new IllegalStateException("bootstrap administrator username is already in use");
        }
        if (StringUtils.hasText(email) && userMapper.selectByEmail(email) != null) {
            throw new IllegalStateException("bootstrap administrator email is already in use");
        }

        LocalDateTime now = LocalDateTime.now();
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(StringUtils.hasText(email) ? email : null);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(1);
        admin.setAccountStatus(1);
        admin.setNickname(username);
        admin.setMembershipId(1);
        admin.setCreatedAt(now);
        admin.setLastLogin(now);
        if (userMapper.insert(admin) != 1) {
            throw new IllegalStateException("bootstrap administrator could not be created");
        }

        recordAudit(username, "created");
        LOGGER.info("Administrator bootstrap created account username={} from environment configuration", username);
    }

    private String property(String key) {
        String value = environment.getProperty(key);
        return value == null ? null : value.trim();
    }

    private void validate(String username, String email, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException("bootstrap administrator requires username and password");
        }
        if (username.length() > 50) {
            throw new IllegalStateException("bootstrap administrator username exceeds 50 characters");
        }
        if (StringUtils.hasText(email) && email.length() > 100) {
            throw new IllegalStateException("bootstrap administrator email exceeds 100 characters");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("bootstrap administrator password must contain at least 12 characters");
        }
    }

    private void recordAudit(String username, String outcome) {
        LocalDateTime now = LocalDateTime.now();
        JSONObject auditValue = new JSONObject();
        auditValue.set("username", username);
        auditValue.set("outcome", outcome);
        auditValue.set("source", "environment");
        auditValue.set("recorded_at", now.toString());

        SystemConfig audit = new SystemConfig();
        audit.setConfigKey(AUDIT_KEY);
        audit.setConfigValue(auditValue.toString());
        audit.setDescription("One-time administrator bootstrap audit marker; never stores the password");
        audit.setCreatedAt(now);
        audit.setUpdatedAt(now);
        systemConfigMapper.insert(audit);
    }
}
