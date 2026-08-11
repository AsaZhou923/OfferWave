package com.offerwave.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerwave.entity.SystemConfig;
import com.offerwave.entity.User;
import com.offerwave.mapper.SystemConfigMapper;
import com.offerwave.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private Environment environment;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments arguments;

    @InjectMocks
    private AdminBootstrapRunner runner;

    @Test
    void shouldCreateAndAuditAdministratorWithoutPersistingPlaintextPassword() {
        configureEnvironment();
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.selectByUsername("bootstrap-admin")).thenReturn(null);
        when(userMapper.selectByEmail("admin@example.com")).thenReturn(null);
        when(passwordEncoder.encode("long-secret-value")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        when(systemConfigMapper.insert(any(SystemConfig.class))).thenReturn(1);

        runner.run(arguments);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(1, userCaptor.getValue().getRole());
        assertEquals("bcrypt-hash", userCaptor.getValue().getPasswordHash());
        assertEquals(1, userCaptor.getValue().getMembershipId());

        ArgumentCaptor<SystemConfig> auditCaptor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigMapper).insert(auditCaptor.capture());
        assertEquals("bootstrap.admin.environment.v1", auditCaptor.getValue().getConfigKey());
        assertFalse(auditCaptor.getValue().getConfigValue().contains("long-secret-value"));
    }

    @Test
    void shouldNotRunAgainAfterAuditMarkerExists() {
        configureEnvironment();
        when(systemConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(new SystemConfig());

        runner.run(arguments);

        verify(userMapper, never()).insert(any(User.class));
        verify(systemConfigMapper, never()).insert(any(SystemConfig.class));
    }

    private void configureEnvironment() {
        when(environment.getProperty("offerwave.bootstrap.admin.username")).thenReturn("bootstrap-admin");
        when(environment.getProperty("offerwave.bootstrap.admin.email")).thenReturn("admin@example.com");
        when(environment.getProperty("offerwave.bootstrap.admin.password")).thenReturn("long-secret-value");
    }
}
