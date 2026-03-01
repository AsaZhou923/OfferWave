package com.offernow.service;

import cn.hutool.core.util.RandomUtil;
import com.offernow.dto.EmailCodeLoginDto;
import com.offernow.dto.RegisterDto;
import com.offernow.dto.ResetPasswordDto;
import com.offernow.dto.SendEmailCodeDto;
import com.offernow.dto.UsernamePasswordLoginDto;
import com.offernow.entity.User;
import com.offernow.mapper.UserMapper;
import com.offernow.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration SEND_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration DAILY_LIMIT_TTL = Duration.ofDays(1);
    private static final long DAILY_LIMIT = 10L;

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

    @Value("${offernow.mail.from:asazhou@qq.com}")
    private String mailFrom;

    @Override
    public Map<String, Object> login(UsernamePasswordLoginDto loginDto) {
        String identifier = loginDto.getUsername() == null ? "" : loginDto.getUsername().trim();
        User user = userMapper.selectByUsername(identifier);
        if (user == null) {
            user = userMapper.selectByEmail(identifier.toLowerCase());
        }
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getAccountStatus())) {
            throw new RuntimeException("账号已被封禁，请联系管理员");
        }
        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);
        return buildLoginResponse(user, false);
    }

    @Override
    public void register(RegisterDto registerDto) {
        if (userMapper.selectByUsername(registerDto.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registerDto.getPassword()));
        user.setNickname("User_" + RandomUtil.randomString(6));
        user.setRole(0);
        user.setAccountStatus(1);
        user.setMembershipId(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public void sendEmailCode(SendEmailCodeDto dto, String clientIp) {
        String email = normalizeEmail(dto.getEmail());
        String type = dto.getType();
        String ip = normalizeIp(clientIp);

        if ("reset_pwd".equals(type) && userMapper.selectByEmail(email) == null) {
            throw new RuntimeException("该邮箱尚未注册");
        }

        enforceSendRateLimit(email, type, ip);
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(codeKey(type, email), code, CODE_TTL);
        sendCodeEmail(email, type, code);
    }

    @Override
    public Map<String, Object> loginByEmailCode(EmailCodeLoginDto dto) {
        String email = normalizeEmail(dto.getEmail());
        verifyEmailCode("login", email, dto.getCode());

        User user = userMapper.selectByEmail(email);
        boolean isNewUser = false;
        if (user == null) {
            String rawPassword = dto.getPassword();
            if (!StringUtils.hasText(rawPassword) || rawPassword.trim().length() < 6) {
                throw new RuntimeException("首次邮箱登录请设置至少6位密码");
            }
            user = buildNewEmailUser(email, rawPassword.trim());
            userMapper.insert(user);
            isNewUser = true;
        }

        if (Integer.valueOf(0).equals(user.getAccountStatus())) {
            throw new RuntimeException("账号已被封禁，请联系管理员");
        }

        user.setLastLogin(LocalDateTime.now());
        userMapper.updateById(user);
        stringRedisTemplate.delete(codeKey("login", email));
        return buildLoginResponse(user, isNewUser);
    }

    @Override
    public void resetPassword(ResetPasswordDto dto) {
        String email = normalizeEmail(dto.getEmail());
        verifyEmailCode("reset_pwd", email, dto.getCode());

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new RuntimeException("该邮箱尚未注册");
        }
        if (Integer.valueOf(0).equals(user.getAccountStatus())) {
            throw new RuntimeException("账号已被封禁，请联系管理员");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        stringRedisTemplate.delete(codeKey("reset_pwd", email));
    }

    private void enforceSendRateLimit(String email, String type, String ip) {
        String cooldownEmailKey = "email_code:cooldown:email:" + type + ":" + email;
        String cooldownIpKey = "email_code:cooldown:ip:" + type + ":" + ip;
        String day = LocalDate.now().toString();
        String emailDailyCountKey = "email_code:count:email:" + type + ":" + day + ":" + email;
        String ipDailyCountKey = "email_code:count:ip:" + type + ":" + day + ":" + ip;

        if (Boolean.FALSE.equals(stringRedisTemplate.opsForValue().setIfAbsent(cooldownEmailKey, "1", SEND_COOLDOWN))) {
            throw new IllegalStateException("同一邮箱发送过于频繁，请1分钟后再试");
        }
        if (Boolean.FALSE.equals(stringRedisTemplate.opsForValue().setIfAbsent(cooldownIpKey, "1", SEND_COOLDOWN))) {
            throw new IllegalStateException("同一IP发送过于频繁，请1分钟后再试");
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
            throw new IllegalStateException("同一邮箱24小时内发送次数已达上限");
        }
        if (ipCount != null && ipCount > DAILY_LIMIT) {
            throw new IllegalStateException("同一IP 24小时内发送次数已达上限");
        }
    }

    private void verifyEmailCode(String type, String email, String code) {
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey(type, email));
        if (!StringUtils.hasText(cachedCode) || !cachedCode.equals(code)) {
            throw new RuntimeException("验证码错误或已过期");
        }
    }

    private void sendCodeEmail(String email, String type, String code) {
        String sceneText = "login".equals(type) ? "登录" : "重置密码";
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("OfferNow 邮箱验证码");
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
        user.setMembershipId(1);
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
        return ("login".equals(type) ? "login_code:" : "reset_pwd_code:") + email;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeIp(String ip) {
        return StringUtils.hasText(ip) ? ip.trim() : "unknown";
    }

    private Map<String, Object> buildLoginResponse(User user, boolean isNewUser) {
        String token = jwtUtil.generateToken(user.getId().toString());
        Map<String, Object> responseData = new HashMap<>();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", null);
        userInfo.put("role", user.getRole());
        userInfo.put("is_admin", Integer.valueOf(1).equals(user.getRole()));
        userInfo.put("membership_level", user.getMembershipId());
        userInfo.put("is_vip", user.getMembershipId() > 1);
        userInfo.put("email", user.getEmail());

        responseData.put("token", token);
        responseData.put("user", userInfo);
        responseData.put("is_new_user", isNewUser);
        return responseData;
    }
}

