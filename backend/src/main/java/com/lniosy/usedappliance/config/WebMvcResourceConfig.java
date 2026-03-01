package com.lniosy.usedappliance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcResourceConfig implements WebMvcConfigurer {
    private final String uploadDir;
    private final RequestMetricsInterceptor requestMetricsInterceptor;

    public WebMvcResourceConfig(@Value("${app.file.upload-dir:uploads}") String uploadDir,
                                RequestMetricsInterceptor requestMetricsInterceptor) {
        this.uploadDir = uploadDir;
        this.requestMetricsInterceptor = requestMetricsInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path abs = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + abs.toString() + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestMetricsInterceptor).addPathPatterns("/api/**");
    }
}
