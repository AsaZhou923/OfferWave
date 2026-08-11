package com.offerwave.service;

import com.offerwave.common.AuthRequestException;
import com.offerwave.dto.EmailCodeLoginDto;
import com.offerwave.dto.ResetPasswordDto;
import com.offerwave.dto.SendEmailCodeDto;
import com.offerwave.dto.UsernamePasswordLoginDto;
import com.offerwave.entity.User;
import com.offerwave.mapper.UserMapper;
import com.offerwave.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MembershipAccessService membershipAccessService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void invalidCodeShouldUseAtomicAttemptLimitedScriptBeforeUserLookup() {
        EmailCodeLoginDto dto = new EmailCodeLoginDto();
        dto.setEmail("User@Example.com");
        dto.setCode("111111");
        when(stringRedisTemplate.execute(
                any(RedisScript.class), anyList(), any(), any(), any())).thenReturn(0L);

        AuthRequestException exception = assertThrows(AuthRequestException.class,
                () -> authService.loginByEmailCode(dto));

        assertEquals(401, exception.getStatusCode());
        assertEquals("登录失败，请检查验证码后重试", exception.getPublicMessage());
        verifyNoInteractions(userMapper);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keyCaptor = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate).execute(
                any(RedisScript.class), keyCaptor.capture(), eq("111111"), eq("5"), eq("300000"));
        List<String> keys = keyCaptor.getValue();
        assertEquals(2, keys.size());
        assertTrue(keys.get(0).contains("{login:user@example.com}"));
        assertTrue(keys.get(1).contains("{login:user@example.com}"));
    }

    @Test
    void unknownPasswordLoginShouldPerformDummyHashCheckAndReturnGenericRejection() {
        UsernamePasswordLoginDto dto = new UsernamePasswordLoginDto();
        dto.setUsername("missing@example.com");
        dto.setPassword("guess");
        when(userMapper.selectByUsername("missing@example.com")).thenReturn(null);
        when(userMapper.selectByEmail("missing@example.com")).thenReturn(null);
        when(passwordEncoder.matches(eq("guess"), anyString())).thenReturn(false);

        AuthRequestException exception = assertThrows(AuthRequestException.class,
                () -> authService.login(dto));

        assertEquals(401, exception.getStatusCode());
        assertEquals("用户名或密码错误", exception.getPublicMessage());
        verify(passwordEncoder).matches(eq("guess"), anyString());
    }

    @Test
    void matchingCodeShouldBeConsumedAndIssueCredentialBoundToken() {
        EmailCodeLoginDto dto = new EmailCodeLoginDto();
        dto.setEmail("user@example.com");
        dto.setCode("123456");
        User user = activeUser();
        when(stringRedisTemplate.execute(
                any(RedisScript.class), anyList(), any(), any(), any())).thenReturn(1L);
        when(userMapper.selectByEmail("user@example.com")).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);
        when(membershipAccessService.ensureMembershipActive(user)).thenReturn(user);
        when(jwtUtil.generateToken(user)).thenReturn("signed-token");

        Map<String, Object> response = authService.loginByEmailCode(dto);

        assertEquals("signed-token", response.get("token"));
        verify(jwtUtil).generateToken(user);
        verify(stringRedisTemplate, never()).delete(startsWith("email_code:"));
    }

    @Test
    void emailCodeRequestShouldUseSameDeliveryPathWithoutLookingUpAccountState() {
        SendEmailCodeDto dto = new SendEmailCodeDto();
        dto.setEmail("missing@example.com");
        dto.setType("reset_pwd");
        ReflectionTestUtils.setField(authService, "mailFrom", "no-reply@offerwave.example");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        authService.sendEmailCode(dto, "203.0.113.10");

        verifyNoInteractions(userMapper);
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(valueOperations).set(
                startsWith("email_code:{reset_pwd:missing@example.com}"),
                anyString(),
                any(Duration.class));
    }

    @Test
    void sendCooldownShouldBeClassifiedAsExpectedRateLimitRejection() {
        SendEmailCodeDto dto = new SendEmailCodeDto();
        dto.setEmail("user@example.com");
        dto.setType("login");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        AuthRequestException exception = assertThrows(AuthRequestException.class,
                () -> authService.sendEmailCode(dto, "203.0.113.10"));

        assertEquals(429, exception.getStatusCode());
        verifyNoInteractions(mailSender);
    }

    @Test
    void resetPasswordShouldPersistNewCredentialAfterAtomicCodeConsumption() {
        ResetPasswordDto dto = new ResetPasswordDto();
        dto.setEmail("user@example.com");
        dto.setCode("123456");
        dto.setNewPassword("new-password");
        User user = activeUser();
        when(stringRedisTemplate.execute(
                any(RedisScript.class), anyList(), any(), any(), any())).thenReturn(1L);
        when(userMapper.selectByEmail("user@example.com")).thenReturn(user);
        when(passwordEncoder.encode("new-password")).thenReturn("new-password-hash");
        when(userMapper.updateById(user)).thenReturn(1);

        authService.resetPassword(dto);

        assertEquals("new-password-hash", user.getPasswordHash());
        verify(userMapper).updateById(user);
    }

    private User activeUser() {
        User user = new User();
        user.setId(9L);
        user.setEmail("user@example.com");
        user.setNickname("Test User");
        user.setRole(0);
        user.setAccountStatus(1);
        user.setMembershipId(2);
        user.setPasswordHash("password-hash");
        return user;
    }
}
