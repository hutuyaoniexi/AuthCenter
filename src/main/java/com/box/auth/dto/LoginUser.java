package com.box.auth.dto;

import com.box.auth.domain.SysUser;

import java.util.Set;

/**
 * 用户信息
 *
 * @author box
 */
public record LoginUser (

    /**
     * 用户唯一标识
     */
     String token,

    /**
     * 用户名id
     */
     Long userId,

    /**
     * 用户名
     */
     String userName,

    /**
     * 登录时间
     */
     Long loginTime,

    /**
     * 过期时间
     */
     Long expireTime,

    /**
     * 登录IP地址
     */
     String ipAddr,

    /**
     * 权限列表
     */
     Set<String> permissions,

    /**
     * 角色列表
     */
     Set<String> roles,

    /** 删除标志（0代表存在 2代表删除） */
    String delFlag,

    /** 帐号状态（0正常 1停用） */
    String status
) {


    public static LoginUser from(SysUser user) {
        return new LoginUser(
                null,                       // token
                user.getUserId(),           // userId
                user.getUserName(),         // userName
                null,                       // loginTime
                null,                       // expireTime
                null,                       // ipaddr
                Set.of(),                   // permissions
                Set.of(),                   // roles
                user.getDelFlag(),          // delFlag
                user.getStatus()         // status
        );
    }
}
