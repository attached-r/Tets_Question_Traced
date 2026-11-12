package com.rj.issuehelper.util;

import com.rj.issuehelper.common.ResultCode;
import com.rj.issuehelper.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * 文本守卫：空白、长度、编号格式校验。
 * <p>
 * 关键约定：{@link #assertContent(String)} 校验通过后返回**原始字符串**，
 * 落库不做任何加工。规范化结果只用于判断"是不是空白"，绝不覆盖原文——
 * 用户贴的报错堆栈里换行和缩进都是排查线索，折叠掉就废了。
 */
public final class TextGuard {

    /** 长度上限（字符数，不是字节数） */
    public static final int CONTENT_MAX_LENGTH = 2000;

    /** 列表摘要长度 */
    public static final int PREVIEW_LENGTH = 80;

    /**
     * 看起来是空白、但 {@code String.strip()} 处理不掉的字符：
     * 不换行空格、窄空格、零宽空格、BOM、全角空格。
     */
    private static final Pattern INVISIBLE = Pattern.compile("[\\u00A0\\u2007\\u202F\\u200B\\uFEFF\\u3000]");

    private static final Pattern REQUEST_ID = Pattern.compile("^[A-Za-z0-9_-]{4,64}$");

    private TextGuard() {
    }

    /**
     * 用于"是否为空"判断的规范化：不可见字符转半角空格 → 连续空白折叠 → 首尾去空白。
     * 结果**不落库**。
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String replaced = INVISIBLE.matcher(raw).replaceAll(" ");
        return replaced.replaceAll("\\s+", " ").trim();
    }

    public static boolean isBlank(String raw) {
        return normalize(raw).isEmpty();
    }

    public static void assertRequestId(String requestId) {
        if (requestId == null || !REQUEST_ID.matcher(requestId.trim()).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "提交编号格式非法（4-64 位字母、数字、下划线或短横线）");
        }
    }

    /**
     * 校验反馈原文，通过后返回原始内容（不是规范化后的内容）。
     *
     * @throws BusinessException 400：为空、纯空白、超长
     */
    public static String assertContent(String content) {
        if (content == null || content.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "反馈内容不能为空");
        }
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "反馈内容不能超过 " + CONTENT_MAX_LENGTH + " 字");
        }
        if (isBlank(content)) {
            // 纯半角空格 / 纯全角空格 / 纯换行 都归到这里
            throw new BusinessException(ResultCode.BAD_REQUEST, "反馈内容不能为空白");
        }
        return content;
    }

    /** 列表摘要：按字符截断，避免把整段内部信息塞进列表响应 */
    public static String preview(String content) {
        if (content == null) {
            return null;
        }
        String oneLine = content.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= PREVIEW_LENGTH ? oneLine : oneLine.substring(0, PREVIEW_LENGTH) + "…";
    }

    /** 可选文本字段的长度校验（备注类） */
    public static void assertOptionalLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + "不能超过 " + max + " 字");
        }
    }
}
