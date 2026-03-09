package com.box.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求 DTO
 *
 * @param username
 * @param password
 */
public record LoginRequest(
        @NotBlank(message = "用户名 不能为空")
        @Size(min = 2, max = 20, message = "用户名长度必须在 2~20 之间")
        String username,

        @NotBlank(message = "用户密码 不能为空")
        @Size(min = 5, max = 20, message = "用户密码长度必须在 5~20 之间")
        String password) {
}
