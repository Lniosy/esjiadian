package com.lniosy.usedappliance.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lniosy.usedappliance.entity.Category;
import com.lniosy.usedappliance.entity.SysUser;
import com.lniosy.usedappliance.mapper.CategoryMapper;
import com.lniosy.usedappliance.mapper.SysUserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataBootstrap implements CommandLineRunner {
    private final SysUserMapper sysUserMapper;
    private final CategoryMapper categoryMapper;
    private final PasswordEncoder passwordEncoder;

    public DataBootstrap(SysUserMapper sysUserMapper, CategoryMapper categoryMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.categoryMapper = categoryMapper;
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
        seedCategories();
    }

    private void seedCategories() {
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>());
        if (count != null && count > 0) {
            return;
        }
        Long major = insertCategory(null, "大家电", 10);
        insertCategory(major, "冰箱", 11);
        insertCategory(major, "洗衣机", 12);
        insertCategory(major, "空调", 13);
        insertCategory(major, "电视", 14);

        Long small = insertCategory(null, "小家电", 20);
        insertCategory(small, "微波炉", 21);
        insertCategory(small, "电饭煲", 22);
        insertCategory(small, "电风扇", 23);
        insertCategory(small, "空气炸锅", 24);

        Long digital = insertCategory(null, "数码电器", 30);
        insertCategory(digital, "笔记本", 31);
        insertCategory(digital, "手机", 32);
        insertCategory(digital, "平板", 33);
        insertCategory(digital, "音响", 34);
    }

    private Long insertCategory(Long parentId, String name, int sort) {
        Category c = new Category();
        c.setParentId(parentId);
        c.setName(name);
        c.setSort(sort);
        categoryMapper.insert(c);
        return c.getId();
    }
}
