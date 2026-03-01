package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.auth.AuthTokenResponse;
import com.lniosy.usedappliance.dto.auth.LoginCodeRequest;
import com.lniosy.usedappliance.dto.auth.LoginPasswordRequest;
import com.lniosy.usedappliance.dto.auth.RegisterRequest;
import com.lniosy.usedappliance.dto.auth.SendCodeResponse;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import com.lniosy.usedappliance.security.JwtTokenProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AuthService {
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCodeService authCodeService;

    public AuthService(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, AuthCodeService authCodeService) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authCodeService = authCodeService;
    }

    public SendCodeResponse sendVerifyCode(String account) {
        return authCodeService.send(account);
    }

    public void register(RegisterRequest req) {
        authCodeService.validateAndConsume(req.account(), req.verifyCode());
        SysUser exists = findByAccount(req.account());
        if (exists != null) {
            throw new BizException(409, "账号已存在");
        }
        SysUser user = new SysUser();
        if (req.account().contains("@")) {
            user.setEmail(req.account());
        } else {
            user.setPhone(req.account());
        }
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setNickname("user" + System.currentTimeMillis() % 100000);
        user.setRoles("ROLE_BUYER");
        user.setAuthStatus("UNVERIFIED");
        user.setLoginFailCount(0);
        user.setEnabled(true);
        sysUserMapper.insert(user);
    }

    public AuthTokenResponse loginByPassword(LoginPasswordRequest req) {
        SysUser user = findByAccount(req.account());
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BizException(401, "账号或密码错误");
        }
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(now)) {
            throw new BizException(423, "账号锁定中，请稍后再试");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            int fails = user.getLoginFailCount() == null ? 1 : user.getLoginFailCount() + 1;
            LocalDateTime lockUntil = fails >= 5 ? now.plusMinutes(30) : null;
            sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, user.getId())
                    .set(SysUser::getLoginFailCount, fails)
                    .set(SysUser::getLockUntil, lockUntil));
            throw new BizException(401, "账号或密码错误");
        }
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getLockUntil, null));
        return buildToken(user);
    }

    public AuthTokenResponse loginByCode(LoginCodeRequest req) {
        authCodeService.validateAndConsume(req.account(), req.verifyCode());
        SysUser user = findByAccount(req.account());
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BizException(404, "账号不存在");
        }
        return buildToken(user);
    }

    private AuthTokenResponse buildToken(SysUser user) {
        String rolesRaw = user.getRoles();
        List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesRaw.split(","))
                .map(SimpleGrantedAuthority::new)
                .toList();
        String username = user.getEmail() != null ? user.getEmail() : user.getPhone();
        String token = jwtTokenProvider.createToken(user.getId(), username, authorities);
        return new AuthTokenResponse(token, user.getId(), username, rolesRaw);
    }

    private SysUser findByAccount(String account) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, account)
                .or()
                .eq(SysUser::getPhone, account)
                .last("limit 1"));
    }
}
