package com.rj.issuehelper.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式化工具。
 * <p>
 * VO 里的时间统一用 {@code String} 输出：
 * 一来避免前端为时区/格式做二次转换，二来不依赖 Jackson 的日期序列化配置。
 */
public final class TimeUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : FORMATTER.format(time);
    }

    public static String now() {
        return FORMATTER.format(LocalDateTime.now());
    }
}
