package com.box.auth.mapper;

import com.box.auth.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 数据层
 *
 * @author box
 */
@Mapper
public interface SysUserMapper {


    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    SysUser selectUserByUserName(String userName);

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
     int updateUser(SysUser user);

}
