package com.lniosy.usedappliance.service;

import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.auth.SendCodeResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class AuthCodeService {
    private static final int EXPIRE_SECONDS = 300;
    private static final String CODE_KEY_PREFIX = "auth:code:";
    private static final Map<String, CodeEntry> CODE_CACHE = new ConcurrentHashMap<>();
    private final StringRedisTemplate stringRedisTemplate;

    public AuthCodeService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public SendCodeResponse send(String account) {
        String normalized = normalizeAccount(account);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        cacheInMemory(normalized, code);
        try {
            stringRedisTemplate.opsForValue().set(redisKey(normalized), code, EXPIRE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Redis 异常时保底走内存缓存，避免验证码流程不可用
        }
        return SendCodeResponse.of(normalized, code, EXPIRE_SECONDS, true);
    }

    public void validateAndConsume(String account, String verifyCode) {
        String normalized = normalizeAccount(account);
        String expected = null;
        try {
            expected = stringRedisTemplate.opsForValue().get(redisKey(normalized));
        } catch (Exception ignored) {
            // Redis 不可用时回退内存校验
        }

        if (expected == null) {
            expected = validateFromMemory(normalized);
        }

        if (expected == null) {
            throw new BizException(400, "验证码不存在或已失效");
        }

        if (!expected.equals(verifyCode)) {
            throw new BizException(400, "验证码错误");
        }

        CODE_CACHE.remove(normalized);
        try {
            stringRedisTemplate.delete(redisKey(normalized));
        } catch (Exception ignored) {
            // 删除失败不影响本次登录/注册校验结果
        }
    }

    private String normalizeAccount(String account) {
        if (account == null || account.isBlank()) {
            throw new BizException(400, "账号不能为空");
        }
        String normalized = account.trim();
        // 手机号格式校验 (11位数字)
        boolean isPhone = normalized.matches("^1[3-9]\\d{9}$");
        // 邮箱格式校验
        boolean isEmail = normalized.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

        if (!isPhone && !isEmail) {
            throw new BizException(400, "账号格式不正确，请输入正确的手机号或邮箱");
        }
        return normalized;
    }

    private String redisKey(String account) {
        return CODE_KEY_PREFIX + account;
    }

    private void cacheInMemory(String account, String code) {
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(EXPIRE_SECONDS);
        CODE_CACHE.put(account, new CodeEntry(code, expireAt));
    }

    private String validateFromMemory(String account) {
        CodeEntry entry = CODE_CACHE.get(account);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt().isBefore(LocalDateTime.now())) {
            CODE_CACHE.remove(account);
            return null;
        }
        return entry.code();
    }

    private record CodeEntry(String code, LocalDateTime expireAt) {
    }
}
