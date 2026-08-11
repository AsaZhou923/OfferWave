package com.offerwave.service;

import cn.hutool.core.util.RandomUtil;
import com.offerwave.common.AuthRequestException;
import com.offerwave.dto.EmailCodeLoginDto;
import com.offerwave.dto.RegisterDto;
import com.offerwave.dto.ResetPasswordDto;
import com.offerwave.dto.SendEmailCodeDto;
import com.offerwave.dto.UsernamePasswordLoginDto;
import com.offerwave.entity.User;
import com.offerwave.mapper.UserMapper;
import com.offerwave.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration DAILY_LIMIT_TTL = Duration.ofDays(1);
    private static final long DAILY_LIMIT = 10L;
    private static final int MAX_CODE_VERIFY_ATTEMPTS = 5;
    private static final int TRIAL_MEMBERSHIP_ID = 2;
    private static final int TRIAL_DAYS = 7;
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cKcLSFnmyMIjZK5ZpAqIzYAYug69S";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> CONSUME_CODE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current then
                return -1
            end
            if current == ARGV[1] then
                redis.call('DEL', KEYS[1])
                redis.call('DEL', KEYS[2])
                return 1
            end
            local attempts = redis.call('INCR', KEYS[2])
            if attempts == 1 then
                redis.call('PEXPIRE', KEYS[2], ARGV[3])
            end
            if attempts >= tonumber(ARGV[2]) then
                redis.call('DEL', KEYS[1])
                redis.call('DEL', KEYS[2])
                return -2
            end
            return 0
            """, Long.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MembershipAccessService membershipAccessService;

    @Value("${offerwave.mail.from:}")
    private String mailFrom;

    @Override
    public Map<String, Object> login(UsernamePasswordLoginDto loginDto) {
        String identifier = loginDto.getUsername() == null ? "" : loginDto.getUsername().trim();
        User user = userMapper.selectByUsername(identifier);
        if (user == null) {
            user = userMapper.selectByEmail(identifier.toLowerCase());
        }
        if (user == null) {
            passwordEncoder.matches(loginDto.getPassword(), DUMMY_PASSWORD_HASH);
            throw AuthRequestException.unauthorized("用户名或密码错误");
        }
        boolean passwordMatches = passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash());
        if (!passwordMatches || Integer.valueOf(0).equals(user.getAccountStatus())) {
            throw AuthRequestException.unauthorized("用户名或密码错误");
        }

        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);
        return buildLoginResponse(user, false);
    }

    @Override
    public void register(RegisterDto registerDto) {
        String email = normalizeEmail(registerDto.getEmail());
        consumeEmailCode("register", email, registerDto.getCode());
        if (userMapper.selectByEmail(email) != null) {
            throw AuthRequestException.badRequest("注册失败，请检查信息后重试");
        }

        User user = buildNewEmailUser(email, registerDto.getPassword().trim());
        userMapper.insert(user);
    }

    @Override
    public void sendEmailCode(SendEmailCodeDto dto, String clientIp) {
        String email = normalizeEmail(dto.getEmail());
        String type = dto.getType();
        String ip = normalizeIp(clientIp);

        enforceSendRateLimit(email, type, ip);
        // Always follow the same delivery path. Account eligibility is checked only
        // after the recipient proves mailbox ownership with the one-time code.
        String code = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
        stringRedisTemplate.delete(attemptKey(type, email));
        stringRedisTemplate.opsForValue().set(codeKey(type, email), code, CODE_TTL);
        try {
            sendCodeEmail(email, type, code);
        } catch (RuntimeException ex) {
            stringRedisTemplate.delete(List.of(codeKey(type, email), attemptKey(type, email)));
            throw ex;
        }
    }

    @Override
    public Map<String, Object> loginByEmailCode(EmailCodeLoginDto dto) {
        String email = normalizeEmail(dto.getEmail());
        consumeEmailCode("login", email, dto.getCode());

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw AuthRequestException.unauthorized("登录失败，请检查验证码后重试");
        }
        if (Integer.valueOf(0).equals(user.getAccountStatus())) {
            throw AuthRequestException.unauthorized("登录失败，请检查验证码后重试");
        }

        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);
        return buildLoginResponse(user, false);
    }

    @Override
    public void resetPassword(ResetPasswordDto dto) {
        String email = normalizeEmail(dto.getEmail());
        consumeEmailCode("reset_pwd", email, dto.getCode());

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw AuthRequestException.badRequest("密码重置失败，请检查信息后重试");
        }
        if (Integer.valueOf(0).equals(user.getAccountStatus())) {
            throw AuthRequestException.badRequest("密码重置失败，请检查信息后重试");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    private void enforceSendRateLimit(String email, String type, String ip) {
        String cooldownEmailKey = "email_code:cooldown:email:" + type + ":" + email;
        String cooldownIpKey = "email_code:cooldown:ip:" + type + ":" + ip;
        String day = LocalDate.now().toString();
        String emailDailyCountKey = "email_code:count:email:" + type + ":" + day + ":" + email;
        String ipDailyCountKey = "email_code:count:ip:" + type + ":" + day + ":" + ip;

        if (Boolean.FALSE.equals(stringRedisTemplate.opsForValue().setIfAbsent(cooldownEmailKey, "1", SEND_COOLDOWN))) {
            throw AuthRequestException.rateLimited();
        }
        if (Boolean.FALSE.equals(stringRedisTemplate.opsForValue().setIfAbsent(cooldownIpKey, "1", SEND_COOLDOWN))) {
            throw AuthRequestException.rateLimited();
        }

        Long emailCount = stringRedisTemplate.opsForValue().increment(emailDailyCountKey);
        if (emailCount != null && emailCount == 1L) {
            stringRedisTemplate.expire(emailDailyCountKey, DAILY_LIMIT_TTL);
        }

        Long ipCount = stringRedisTemplate.opsForValue().increment(ipDailyCountKey);
        if (ipCount != null && ipCount == 1L) {
            stringRedisTemplate.expire(ipDailyCountKey, DAILY_LIMIT_TTL);
        }

        if (emailCount != null && emailCount > DAILY_LIMIT) {
            throw AuthRequestException.rateLimited();
        }
        if (ipCount != null && ipCount > DAILY_LIMIT) {
            throw AuthRequestException.rateLimited();
        }
    }

    private void consumeEmailCode(String type, String email, String code) {
        Long result = stringRedisTemplate.execute(
                CONSUME_CODE_SCRIPT,
                List.of(codeKey(type, email), attemptKey(type, email)),
                code,
                Integer.toString(MAX_CODE_VERIFY_ATTEMPTS),
                Long.toString(CODE_TTL.toMillis()));
        if (!Long.valueOf(1L).equals(result)) {
            if ("login".equals(type)) {
                throw AuthRequestException.unauthorized("登录失败，请检查验证码后重试");
            }
            if ("register".equals(type)) {
                throw AuthRequestException.badRequest("注册失败，请检查信息后重试");
            }
            throw AuthRequestException.badRequest("密码重置失败，请检查信息后重试");
        }
    }

    private void sendCodeEmail(String email, String type, String code) {
        String sceneText = "register".equals(type) ? "注册"
                : ("login".equals(type) ? "登录" : "重置密码");
        if (!StringUtils.hasText(mailFrom)) {
            throw new IllegalStateException("未配置发件邮箱，请设置 MAIL_FROM 或 MAIL_USERNAME");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("OfferWave 邮箱验证码");
        message.setText("您正在进行" + sceneText + "操作，验证码为：" + code + "，5分钟内有效。");
        mailSender.send(message);
    }

    private User buildNewEmailUser(String email, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(generateUniqueUsername(email));
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setNickname("User_" + RandomUtil.randomString(6));
        user.setRole(0);
        user.setAccountStatus(1);
        user.setMembershipId(TRIAL_MEMBERSHIP_ID);
        user.setMembershipExpireAt(LocalDateTime.now().plusDays(TRIAL_DAYS));
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        return user;
    }

    private String generateUniqueUsername(String email) {
        String localPart = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        if (!StringUtils.hasText(localPart)) {
            localPart = "email_user";
        }
        localPart = localPart.length() > 16 ? localPart.substring(0, 16) : localPart;
        for (int i = 0; i < 10; i++) {
            String candidate = localPart + "_" + RandomUtil.randomString(6);
            if (userMapper.selectByUsername(candidate) == null) {
                return candidate;
            }
        }
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String codeKey(String type, String email) {
        return "email_code:{" + type + ":" + email + "}:value";
    }

    private String attemptKey(String type, String email) {
        return "email_code:{" + type + ":" + email + "}:attempts";
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeIp(String ip) {
        return StringUtils.hasText(ip) ? ip.trim() : "unknown";
    }

    private Map<String, Object> buildLoginResponse(User user, boolean isNewUser) {
        User effectiveUser = membershipAccessService.ensureMembershipActive(user);
        String token = jwtUtil.generateToken(effectiveUser);
        Map<String, Object> responseData = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", effectiveUser.getId());
        userInfo.put("nickname", effectiveUser.getNickname());
        userInfo.put("avatar", null);
        userInfo.put("role", effectiveUser.getRole());
        userInfo.put("is_admin", Integer.valueOf(1).equals(effectiveUser.getRole()));
        userInfo.put("membership_level", effectiveUser.getMembershipId());
        userInfo.put("is_vip", membershipAccessService.isVip(effectiveUser));
        userInfo.put("email", effectiveUser.getEmail());

        responseData.put("token", token);
        responseData.put("user", userInfo);
        responseData.put("is_new_user", isNewUser);
        return responseData;
    }
}
