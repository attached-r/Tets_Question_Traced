package com.rj.issuehelper.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.rj.issuehelper.common.ResultCode;
import com.rj.issuehelper.common.UserAuth;
import com.rj.issuehelper.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具：签发与校验 token。
 * <p>
 * 载荷只携带身份与角色（uid / uname / real / role），不带权限列表。
 * 取舍：角色变更或禁用账号后旧 token 在有效期内仍可用（见开发文档 14.3 R2），
 * 生产应改为短 TTL + refresh token 或服务端吊销名单。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /** 有效期，单位秒 */
    @Value("${jwt.timeout}")
    private long timeout;

    public String createToken(UserAuth user) {
        Instant now = Instant.now();
        return JWT.create()
                .withClaim("uid", user.getUserId())
                .withClaim("uname", user.getUsername())
                .withClaim("real", user.getRealName())
                .withClaim("role", user.getRoleCode())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(timeout)))
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * 校验并解析 token。
     *
     * @throws BusinessException 401：签名不对、格式错误或已过期
     */
    public UserAuth parse(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        try {
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
            return new UserAuth(
                    jwt.getClaim("uid").asLong(),
                    jwt.getClaim("uname").asString(),
                    jwt.getClaim("real").asString(),
                    jwt.getClaim("role").asString()
            );
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
    }

    public long getTimeout() {
        return timeout;
    }
}
