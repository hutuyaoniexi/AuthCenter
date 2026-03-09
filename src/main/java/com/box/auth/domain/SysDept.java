package com.box.auth.domain;


import com.box.common.core.web.domain.BaseEntity;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门表 sys_dept
 *
 * @author box
 */
@Data
@EqualsAndHashCode(callSuper = false) // equals/hashCode 忽略父类字段。
public class SysDept extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    private Long deptId;

    /** 父部门ID */
    private Long parentId;

    /** 祖级列表 */
    private String ancestors;

    /** 部门名称 */
    @NotBlank(message = "部门名称不能为空")
    @Min(value = 0,message = "部门名称 必须大于0个字符")
    @Max(value = 30, message = "部门名称 长度不能超过30个字符")
    private String deptName;

    /** 显示顺序 */
    @NotNull(message = "显示顺序不能为空")
    private Integer orderNum;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    @Min(value = 0,message = "联系电话 必须大于0个字符")
    @Max(value = 11, message = "联系电话 长度不能超过11个字符")
    private String phone;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Min(value = 0,message = "邮箱 长度必须大于0个字符")
    @Max(value = 50, message = "邮箱 长度不能超过50个字符")
    private String email;

    /** 部门状态:0正常,1停用 */
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 父部门名称 */
    private String parentName;

    /** 子部门 */
    private List<SysDept> children = new ArrayList<SysDept>();
}
