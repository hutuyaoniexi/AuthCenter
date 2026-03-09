package com.box.auth.controller;


import com.box.auth.dto.LoginRequest;
import com.box.auth.dto.LoginUser;
import com.box.auth.dto.TokenPair;
import com.box.auth.service.AuthService;
import com.box.common.core.response.Result;
import com.box.common.security.constant.SecurityConstants;
import com.box.common.web.util.CookieUtils;
import com.box.common.web.util.WebTokenResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



/**
 * 鉴权接口
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    /**
     * 登录
     * 成功后：
     * 1. 写 access / refresh cookie
     * 2. 同时返回 token pair，便于未来接口对接
     */
    @PostMapping("/login")
    public Result<TokenPair> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        LoginUser user = authService.login(req.username(), req.password());
        TokenPair tokens = authService.issueTokens(user);
        CookieUtils.writeTokenCookies(response,tokens.accessToken(),tokens.refreshToken());
        return Result.ok(tokens);
    }



    /**
     * 登出
     * access token：先 Authorization，再 Cookie
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = WebTokenResolver.resolveToken(request,SecurityConstants.ACCESS_TOKEN_COOKIE);
        authService.logout(accessToken);
        CookieUtils.clearTokenCookies(response);
        return Result.ok();
    }


    /**
     * 刷新令牌
     * refresh token：优先 Cookie，再回退 Authorization
     * 避免前端统一附带 access token 的 Authorization 头时被误判为 refresh token。
     */
    @PostMapping("/refresh")
    public Result<TokenPair> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(request);
        TokenPair tokens = authService.refresh(refreshToken);
        CookieUtils.writeTokenCookies(response,tokens.accessToken(),tokens.refreshToken());
        return Result.ok(tokens);
    }


    /**
     * 检查 access token 是否有效
     * access token：先 Authorization，再 Cookie
     */
    @GetMapping("/check")
    public ResponseEntity<Void> check(HttpServletRequest request) {
        String accessToken = WebTokenResolver.resolveToken(request,SecurityConstants.ACCESS_TOKEN_COOKIE);
        boolean valid = authService.checkAccessToken(accessToken);

        return valid
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SecurityConstants.REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return WebTokenResolver.resolveToken(request, SecurityConstants.REFRESH_TOKEN_COOKIE);
    }



}
