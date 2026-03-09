package com.box.auth.service;

import com.box.auth.domain.SysLogininfor;
import com.box.auth.domain.SysUser;
import com.box.auth.dto.LoginUser;
import com.box.auth.dto.TokenPair;
import com.box.auth.mapper.SysLogininforMapper;
import com.box.auth.mapper.SysUserMapper;
import com.box.common.cache.constant.CacheConstants;
import com.box.common.cache.redis.RedisService;
import com.box.common.core.constant.CommonConstants;
import com.box.common.core.enums.ErrorCode;
import com.box.common.core.enums.UserStatus;
import com.box.common.core.exception.BaseException;
import com.box.common.core.text.Convert;
import com.box.common.core.util.DateUtils;
import com.box.common.core.util.StringUtils;
import com.box.common.security.token.JwtTokenClaims;
import com.box.common.security.token.JwtTokenParser;
import com.box.common.security.token.JwtTokenProvider;
import com.box.common.security.util.SecurityUtils;
import com.box.common.web.util.IpUtils;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;


@Service
public class AuthService {
    private final SysUserMapper userMapper;
    private final SysLogininforMapper logininforMapper;
    private final RedisService redisService;
    private final JwtTokenParser jwtTokenParser;
    private final JwtTokenProvider jwtTokenProvider;


    public AuthService(SysUserMapper userMapper, SysLogininforMapper logininforMapper, RedisService redisService, JwtTokenParser jwtTokenParser, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.logininforMapper = logininforMapper;
        this.redisService = redisService;
        this.jwtTokenParser = jwtTokenParser;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    public LoginUser login(String username, String password) {
        // IP黑名单校验
        String blackStr = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_LOGIN_BLACKIPLIST));
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr())) {
            throw new BaseException(ErrorCode.FORBIDDEN, "很遗憾，访问IP已被列入系统黑名单");
        }

        SysUser user  = userMapper.selectUserByUserName(username);
        if (user == null) {
            throw new BaseException(ErrorCode.INVALID_PARAM, "用户名或密码错误");
        }
        LoginUser userResult = LoginUser.from(user);

        if (UserStatus.DELETED.getCode().equals(userResult.delFlag())) {
            throw new BaseException(ErrorCode.NOT_FOUND, "对不起，您的账号：" + username + " 已被删除");
        }

        if (UserStatus.DISABLE.getCode().equals(userResult.status())) {
            throw new BaseException(ErrorCode.FORBIDDEN, "对不起，您的账号：" + username + " 已停用");
        }

        validate(userResult.userName(), user.getPassword(), password);
        recordLogininfor(username, CommonConstants.LOGIN_SUCCESS, "登录成功");
        recordLoginInfo(userResult.userId());

        //        return new AuthUser(
//                userResult.userid(),
//                userResult.username(),
//                List.of("api:add", "api:query", "ROLE_ADMIN")
//        );
        return userResult;
    }

    /**
     * 颁发 token
     */
    public TokenPair issueTokens(LoginUser user) {
        String access = jwtTokenProvider.generateAccessToken(
                user.userId(),
                user.userName(),
                user.roles()
        );
        // 解析 jti
        Claims claims = jwtTokenProvider.parseAndValidate(access);
        String jti = claims.getId();

        String refresh = jwtTokenProvider.generateRefreshToken(user.userId(),jti);

        // 缓存登录态
        cacheLoginUser(jti,user);
        return new TokenPair(access, refresh);
    }


    /**
     * 登出
     * 1. 解析 access token
     * 2. 删除服务端登录态
     * 3. 记录退出日志
     * 4. 清空 cookie
     */
    public void logout(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new BaseException(ErrorCode.MISSING_PARAM,"token不存在");
        }

        // 删除用户缓存记录
        JwtTokenClaims tokenClaims = jwtTokenParser.parseAccessToken(accessToken);
        String jti = tokenClaims.getJti();
        if (StringUtils.hasText(jti)) {
            redisService.deleteObject(getTokenKey(jti));
        }

        // 记录用户退出日志
        recordLogininfor(tokenClaims.getUsername(), CommonConstants.LOGOUT, "退出成功");
    }

    /**
     * 刷新 token
     * 1. 校验 refresh token
     * 2. 根据 userId 找登录态
     * 3. 重新签发 access/refresh
     * 4. 更新缓存有效期
     * 5. 重写 cookie
     */
    public TokenPair refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BaseException(ErrorCode.MISSING_PARAM,"token不存在");
        }

        JwtTokenClaims tokenClaims = jwtTokenParser.parseRefreshToken(refreshToken);
        String jti = tokenClaims.getSessionJti();

        if (!StringUtils.hasText(jti)) {
            throw new BaseException(ErrorCode.UNAUTHORIZED, "refresh token无效");
        }

        LoginUser user = redisService.getCacheObject(getTokenKey(jti));
        if (user == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED, "登录状态不存在或已过期");
        }
        TokenPair tokens = issueTokens(user);

        // 清除旧token缓存
        redisService.deleteObject(getTokenKey(jti));

        return tokens;
    }

    /**
     * 检查 access token 是否有效
     * 当前先按：
     * 1. token 本身合法
     * 2. 类型正确
     * 3. 用户登录态仍在 redis
     */
    public boolean checkAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return false;
        }

        try {
            Claims claims = jwtTokenProvider.parseAndValidate(accessToken);
            jwtTokenProvider.validateAudience(claims);
            jwtTokenProvider.validateType(claims, JwtTokenProvider.TYP_ACCESS);

            String jti = claims.getId();
            return StringUtils.hasText(jti) && redisService.hasKey(getTokenKey(jti));
        } catch (IllegalArgumentException e) {
            return false;
        }

    }


    /**
     * 缓存登录用户
     */
    private void cacheLoginUser(String jti,LoginUser user) {
        redisService.setCacheObject(
                getTokenKey(jti),
                user,
                CacheConstants.EXPIRATION,
                TimeUnit.MINUTES
        );
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        // 更新用户登录IP
        sysUser.setLoginIp(IpUtils.getIpAddr());
        // 更新用户登录时间
        sysUser.setLoginDate(DateUtils.getNowDate());
        userMapper.updateUser(sysUser);
    }

    /**
     * 密码校验
     * @param username
     * @param password
     * @param rawPassword
     */
    public void validate(String username,String password, String rawPassword)
    {
        int maxRetryCount = CacheConstants.PASSWORD_MAX_RETRY_COUNT;
        Long lockTime = CacheConstants.PASSWORD_LOCK_TIME;

        Integer retryCount = redisService.getCacheObject(getCacheKey(username));
        if (retryCount == null) {
            retryCount = 0;
        }

        if (retryCount >= Integer.valueOf(maxRetryCount).intValue()) {
            String errMsg = String.format("密码输入错误%s次，帐户锁定%s分钟", maxRetryCount, lockTime);
            throw new BaseException(ErrorCode.INVALID_PARAM,errMsg);
        }

        if (!SecurityUtils.matchesPassword(rawPassword,password)) {
            retryCount = retryCount + 1;
            redisService.setCacheObject(getCacheKey(username), retryCount, lockTime, TimeUnit.MINUTES);
            throw new BaseException(ErrorCode.INVALID_PARAM, "用户名或密码错误");
        } else {
            if (redisService.hasKey(getCacheKey(username))) {
                redisService.deleteObject(getCacheKey(username));
            }
        }
    }


    /**
     * 登录账户密码错误次数缓存键名
     *
     * @param username 用户名
     * @return 缓存键key
     */
    private String getCacheKey(String username) {
        return CacheConstants.PWD_ERR_CNT_KEY + username;
    }

    /**
     * token 缓存 key
     */
    private String getTokenKey(String jti) {
        return CacheConstants.LOGIN_TOKEN_KEY + jti;
    }


    /**
     * 记录登录信息
     *
     * @param username 用户名
     * @param status 状态
     * @param message 消息内容
     * @return
     */
    public void recordLogininfor(String username, String status, String message)
    {
        SysLogininfor logininfor = new SysLogininfor();
        logininfor.setUserName(username);
        logininfor.setIpAddr(IpUtils.getIpAddr());
        logininfor.setMsg(message);

        // 日志状态
        Set<String> SUCCESS_STATUS = Set.of(CommonConstants.LOGIN_SUCCESS,
                                            CommonConstants.LOGOUT,
                                            CommonConstants.REGISTER);

        if (SUCCESS_STATUS.contains(status)) {
            logininfor.setStatus(CommonConstants.LOGIN_SUCCESS_STATUS);
        } else if (CommonConstants.LOGIN_FAIL.equals(status)) {
            logininfor.setStatus(CommonConstants.LOGIN_FAIL_STATUS);
        }

        logininforMapper.insertLogininfor(logininfor);
    }
}
