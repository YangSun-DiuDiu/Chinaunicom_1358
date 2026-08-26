package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.VisitorDeptQrcode;

/**
 * 部门访客登记二维码配置 业务层
 *
 * @author ruoyi
 */
public interface IVisitorDeptQrcodeService
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
     * 通过登记码查询配置（H5公开访问）
     *
     * @param registerCode 登记码
     * @return 配置信息，不存在或停用/删除返回null
     */
    public VisitorDeptQrcode selectByCode(String registerCode);

    /**
     * 新增配置（自动生成登记码，校验部门唯一）
     *
     * @param qrcode 配置信息
     * @return 结果
     */
    public int insertDeptQrcode(VisitorDeptQrcode qrcode);

    /**
     * 修改配置（仅允许修改启用状态等，部门不允许变更）
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
     * 为尚未配置登记二维码的部门批量生成配置
     *
     * @return 生成数量
     */
    public int initAllDepts();

    /**
     * 查询某部门(含子部门)可预约的真实员工列表
     *
     * @param deptId 部门ID
     * @return 员工信息集合
     */
    public List<Map<String, Object>> selectHostUsersByDept(Long deptId);

    /**
     * 判断被访人所在部门是否在登记码限定部门(含子部门)范围内
     *
     * @param hostDeptId 被访人部门ID
     * @param qrDeptId   登记码限定部门ID
     * @return true=在范围内 false=不在范围内
     */
    public boolean isDeptWithinScope(Long hostDeptId, Long qrDeptId);
}
