package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.VisitorDeptQrcode;

/**
 * 部门访客登记二维码配置表 数据层
 *
 * @author ruoyi
 */
public interface VisitorDeptQrcodeMapper
{
    /**
     * 查询部门登记二维码配置列表
     *
     * @param qrcode 配置信息
     * @return 配置集合
     */
    public List<VisitorDeptQrcode> selectDeptQrcodeList(VisitorDeptQrcode qrcode);

    /**
     * 通过主键查询配置
     *
     * @param id 主键ID
     * @return 配置信息
     */
    public VisitorDeptQrcode selectDeptQrcodeById(Long id);

    /**
     * 通过登记码查询配置（H5公开访问，仅返回启用且未删除）
     *
     * @param registerCode 登记码
     * @return 配置信息，不存在或已删除返回null
     */
    public VisitorDeptQrcode selectDeptQrcodeByCode(String registerCode);

    /**
     * 通过部门ID查询配置（含已删除，用于唯一性校验）
     *
     * @param deptId 部门ID
     * @return 配置信息
     */
    public VisitorDeptQrcode selectDeptQrcodeByDeptId(Long deptId);

    /**
     * 新增配置
     *
     * @param qrcode 配置信息
     * @return 结果
     */
    public int insertDeptQrcode(VisitorDeptQrcode qrcode);

    /**
     * 修改配置
     *
     * @param qrcode 配置信息
     * @return 结果
     */
    public int updateDeptQrcode(VisitorDeptQrcode qrcode);

    /**
     * 批量删除配置（逻辑删除）
     *
     * @param ids 主键ID数组
     * @return 结果
     */
    public int deleteDeptQrcodeByIds(Long[] ids);

    /**
     * 查询某部门(含子部门)可预约的真实员工列表
     *
     * @param deptId 部门ID（包含自身及全部子部门）
     * @return 员工信息集合（hostName/hostAccount/hostDept/hostPhone/deptId）
     */
    public List<Map<String, Object>> selectHostUsersByDept(Long deptId);

    /**
     * 查询某部门及其全部子部门的ID集合
     *
     * @param deptId 部门ID
     * @return 部门ID集合（含自身）
     */
    public List<Long> selectDeptDescendantIds(Long deptId);
}
