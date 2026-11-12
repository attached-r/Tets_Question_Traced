package com.rj.issuehelper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 只注册密码编码器，不引入 Spring Security 的过滤链。
 * <p>
 * 引 {@code spring-boot-starter-security} 会默认拦住所有接口并生成随机密码，
 * 在 3 小时的 demo 里只会把时间花在放行配置上。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
