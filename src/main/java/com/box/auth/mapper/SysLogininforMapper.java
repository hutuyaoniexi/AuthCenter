package com.box.auth.mapper;


import com.box.auth.domain.SysLogininfor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统访问日志情况信息 数据层
 *
 * @author box
 */
@Mapper
public interface SysLogininforMapper
{
    /**
     * 新增系统登录日志
     *
     * @param logininfor 访问日志对象
     */
     int insertLogininfor(SysLogininfor logininfor);

}
