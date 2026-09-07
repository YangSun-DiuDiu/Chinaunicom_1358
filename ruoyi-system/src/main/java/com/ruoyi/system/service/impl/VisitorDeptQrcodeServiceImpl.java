package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.VisitorDeptQrcode;
import com.ruoyi.system.mapper.VisitorDeptQrcodeMapper;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.IVisitorDeptQrcodeService;

/**
 * 部门访客登记二维码配置 服务层处理
 *
 * @author ruoyi
 */
@Service
public class VisitorDeptQrcodeServiceImpl implements IVisitorDeptQrcodeService
{
    private static final Logger log = LoggerFactory.getLogger(VisitorDeptQrcodeServiceImpl.class);

    /** 启用状态值 */
    private static final String ENABLED = "1";

    @Autowired
    private VisitorDeptQrcodeMapper deptQrcodeMapper;

    @Autowired
    private ISysDeptService sysDeptService;

    /**
     * 查询部门登记二维码配置列表
     */
    @Override
    public List<VisitorDeptQrcode> selectDeptQrcodeList(VisitorDeptQrcode qrcode)
    {
        return deptQrcodeMapper.selectDeptQrcodeList(qrcode);
    }

    /**
     * 通过主键查询配置
     */
    @Override
    public VisitorDeptQrcode selectDeptQrcodeById(Long id)
    {
        return deptQrcodeMapper.selectDeptQrcodeById(id);
    }

    /**
     * 通过登记码查询配置（H5公开访问）
     */
    @Override
    public VisitorDeptQrcode selectByCode(String registerCode)
    {
        if (StringUtils.isEmpty(registerCode))
        {
            return null;
        }
        return deptQrcodeMapper.selectDeptQrcodeByCode(registerCode);
    }

    /**
     * 通过部门ID查询配置（含未启用/已删除，供当前部门登记码查询）
     */
    @Override
    public VisitorDeptQrcode selectDeptQrcodeByDeptId(Long deptId)
    {
        if (deptId == null)
        {
            return null;
        }
        return deptQrcodeMapper.selectDeptQrcodeByDeptId(deptId);
    }

    /**
     * 新增配置：自动生成登记码，校验部门唯一
     */
    @Override
    public int insertDeptQrcode(VisitorDeptQrcode qrcode)
    {
        // 部门唯一性校验（含已删除，防止重复创建）
        if (qrcode.getDeptId() == null)
        {
            throw new ServiceException("部门不能为空");
        }
        VisitorDeptQrcode exist = deptQrcodeMapper.selectDeptQrcodeByDeptId(qrcode.getDeptId());
        if (exist != null)
        {
            throw new ServiceException("该部门已配置登记二维码，请勿重复添加");
        }
        // 自动生成登记码（16位，不可猜测，避免暴露部门ID）
        qrcode.setRegisterCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        if (StringUtils.isEmpty(qrcode.getEnabled()))
        {
            qrcode.setEnabled(ENABLED);
        }
        return deptQrcodeMapper.insertDeptQrcode(qrcode);
    }

    /**
     * 修改配置：仅允许修改启用状态，部门与登记码不允许变更
     */
    @Override
    public int updateDeptQrcode(VisitorDeptQrcode qrcode)
    {
        if (qrcode.getId() == null)
        {
            throw new ServiceException("配置ID不能为空");
        }
        // 锁定原有部门与登记码，避免误改导致已张贴二维码失效
        VisitorDeptQrcode exist = deptQrcodeMapper.selectDeptQrcodeById(qrcode.getId());
        if (exist == null)
        {
            throw new ServiceException("配置不存在");
        }
        qrcode.setDeptId(exist.getDeptId());
        qrcode.setRegisterCode(exist.getRegisterCode());
        return deptQrcodeMapper.updateDeptQrcode(qrcode);
    }

    /**
     * 批量删除配置
     */
    @Override
    public int deleteDeptQrcodeByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的记录");
        }
        return deptQrcodeMapper.deleteDeptQrcodeByIds(ids);
    }

    /**
     * 为尚未配置登记二维码的部门批量生成配置
     */
    @Override
    public int initAllDepts()
    {
        List<SysDept> depts = sysDeptService.selectDeptList(new SysDept());
        if (depts == null || depts.isEmpty())
        {
            return 0;
        }
        int count = 0;
        for (SysDept dept : depts)
        {
            if (dept.getDeptId() == null)
            {
                continue;
            }
            // 已配置的部门跳过
            if (deptQrcodeMapper.selectDeptQrcodeByDeptId(dept.getDeptId()) != null)
            {
                continue;
            }
            VisitorDeptQrcode qrcode = new VisitorDeptQrcode();
            qrcode.setDeptId(dept.getDeptId());
            qrcode.setRegisterCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
            qrcode.setEnabled(ENABLED);
            qrcode.setCreateBy("admin");
            deptQrcodeMapper.insertDeptQrcode(qrcode);
            count++;
        }
        log.info("部门登记二维码批量初始化完成，本次生成 {} 个", count);
        return count;
    }

    /**
     * 查询某部门(含子部门)可预约的真实员工列表
     */
    @Override
    public List<Map<String, Object>> selectHostUsersByDept(Long deptId)
    {
        return deptQrcodeMapper.selectHostUsersByDept(deptId);
    }

    /**
     * 判断被访人所在部门是否在登记码限定部门(含子部门)范围内
     */
    @Override
    public boolean isDeptWithinScope(Long hostDeptId, Long qrDeptId)
    {
        if (hostDeptId == null || qrDeptId == null)
        {
            return false;
        }
        List<Long> scopeDeptIds = deptQrcodeMapper.selectDeptDescendantIds(qrDeptId);
        return scopeDeptIds != null && scopeDeptIds.contains(hostDeptId);
    }
}
