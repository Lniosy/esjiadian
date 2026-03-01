package com.lniosy.usedappliance.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.lniosy.usedappliance.mapper")
public class MybatisConfig {
}
