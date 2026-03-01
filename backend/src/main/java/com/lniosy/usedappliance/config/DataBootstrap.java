package com.lniosy.usedappliance.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataBootstrap implements CommandLineRunner {
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public DataBootstrap(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        SysUser admin = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, "admin@example.com")
                .last("limit 1"));
        if (admin == null) {
            SysUser user = new SysUser();
            user.setEmail("admin@example.com");
            user.setPasswordHash(passwordEncoder.encode("Admin1234"));
            user.setNickname("系统管理员");
            user.setBio("平台管理员账号");
            user.setRoles("ROLE_ADMIN,ROLE_BUYER,ROLE_SELLER");
            user.setAuthStatus("APPROVED");
            user.setLoginFailCount(0);
            user.setEnabled(true);
            sysUserMapper.insert(user);
        }
    }
}
