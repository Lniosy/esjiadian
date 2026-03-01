package com.lniosy.usedappliance.service;

import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.auth.SendCodeResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthCodeService {
    private static final int EXPIRE_SECONDS = 300;
    private static final Map<String, CodeEntry> CODE_CACHE = new ConcurrentHashMap<>();

    public SendCodeResponse send(String account) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(EXPIRE_SECONDS);
        CODE_CACHE.put(account.trim(), new CodeEntry(code, expireAt));
        return new SendCodeResponse(account, code, EXPIRE_SECONDS, true);
    }

    public void validateAndConsume(String account, String verifyCode) {
        String key = account.trim();
        CodeEntry entry = CODE_CACHE.get(key);
        if (entry == null) {
            throw new BizException(400, "验证码不存在或已失效");
        }
        if (entry.expireAt().isBefore(LocalDateTime.now())) {
            CODE_CACHE.remove(key);
            throw new BizException(400, "验证码已过期");
        }
        if (!entry.code().equals(verifyCode)) {
            throw new BizException(400, "验证码错误");
        }
        CODE_CACHE.remove(key);
    }

    private record CodeEntry(String code, LocalDateTime expireAt) {
    }
}
