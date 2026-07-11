# 19-设备管理-SNMP管理测试

## 1. 测试概述

| 项目 | 说明 |
|------|------|
| 模块名称 | SNMP管理 |
| 涉及页面 | `views/device/snmp/index.vue` |
| API 接口 | 5个（info、ports、nvrStorage、get、walk） |
| 权限要求 | `device:snmp:query` |

### API 清单

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/device/snmp/info/{deviceId}` | `device:snmp:query` | 获取设备SNMP基本信息 |
| GET | `/device/snmp/ports/{deviceId}` | `device:snmp:query` | 获取设备端口状态 |
| GET | `/device/snmp/nvrStorage/{deviceId}` | `device:snmp:query` | 获取NVR存储信息 |
| GET | `/device/snmp/get/{deviceId}` | `device:snmp:query` | 自定义OID SNMP GET |
| GET | `/device/snmp/walk/{deviceId}` | `device:snmp:query` | 自定义OID SNMP Walk |

## 2. 前置条件

- 以 admin 账号登录
- 至少有一台设备配置了正确的SNMP团体名
- 设备支持SNMP v2c协议

## 3. 页面元素检查

| 元素 | 类型 | 检查项 |
|------|------|------|
| 设备选择器 | el-select | 下拉选择设备（仅显示配置了SNMP的设备） |
| 刷新按钮 | el-button | 重新查询SNMP信息 |
| SNMP基本信息区域 | 信息展示 | 系统名称、描述、运行时间等 |
| 端口状态表格 | el-table | 端口号、名称、类型、状态、速率等 |
| NVR存储信息 | 信息展示 | 存储总量、已用、可用、使用率 |
| OID输入框 | el-input | 输入自定义OID |
| SNMP GET按钮 | el-button | 执行SNMP GET查询 |
| SNMP Walk按钮 | el-button | 执行SNMP Walk遍历 |
| 自定义查询结果 | el-table/textarea | 显示查询结果 |
| MIB OID 快捷选择 | 预定义列表或下拉 | 常见OID快捷选择 |

## 4. 正向测试用例

| 用例ID | 测试名称 | 步骤 | 预期结果 |
|--------|----------|------|----------|
| SNMP-001 | 页面加载 | 进入SNMP管理页面 | 设备选择器加载，默认选中第一台设备 |
| SNMP-002 | 选择设备 | 1.选择一台设备 | 自动查询并显示该设备的SNMP信息 |
| SNMP-003 | 基本信息展示 | 1.选择设备后查看基本信息区 | 显示sysName、sysDescr、sysUpTime等 |
| SNMP-004 | 端口状态表格 | 1.查看端口状态区域 | 显示端口索引、名称、类型、状态、速率等 |
| SNMP-005 | 端口状态标签 | 查看端口状态列 | UP显示绿色，DOWN显示红色 |
| SNMP-006 | ifType显示 | 查看端口类型列 | 正确映射ifType数字到类型名称（如6=ethernetCsmacd） |
| SNMP-007 | NVR存储信息 | 1.选择NVR类型的设备 | 显示存储总量/已用/可用/使用率 |
| SNMP-008 | MIB快捷OID查询 | 1.选择预定义的OID快捷选项 2.点击GET | 返回对应OID的值 |
| SNMP-009 | 自定义OID GET | 1.输入OID如`.1.3.6.1.2.1.1.5.0` 2.点击GET | 返回OID对应的值 |
| SNMP-010 | 自定义OID Walk | 1.输入OID前缀 2.点击Walk | 遍历该OID子树，返回所有子OID和值 |
| SNMP-011 | 刷新信息 | 1.点击刷新按钮 | 重新查询并更新所有数据 |

## 5. 边界与异常测试

| 用例ID | 测试名称 | 条件/步骤 | 预期结果 |
|--------|----------|-----------|----------|
| SNMP-B01 | 设备SNMP不可达 | 1.选择SNMP端口不通的设备 | 查询超时提示"SNMP连接失败" |
| SNMP-B02 | 团体名错误 | 1.设备SNMP团体名不正确 | 查询返回空或失败提示 |
| SNMP-B03 | 无效OID查询 | 1.输入不存在的OID 2.点击GET | 返回"No Such Object"或空值 |
| SNMP-B04 | NVR设备选其他类型 | 在非NVR设备查看NVR存储 | 无存储信息或返回空 |
| SNMP-B05 | 端口过多设备 | 查看有大量端口的交换机 | 表格加载正常，不卡顿 |
| SNMP-E01 | 无SNMP设备 | 所有设备的SNMP都未配置或不可达 | 下拉为空或查询无结果 |
| SNMP-E02 | SNMP版本v3 | 选择v3版本的设备 | v3认证参数是否正确处理 |

## 6. 权限测试

| 用例ID | 条件 | 预期结果 |
|--------|------|----------|
| SNMP-P01 | 无`device:snmp:query` | 页面/菜单不可见 |

## 7. 测试记录表

| 用例ID | 状态 | 测试人 | 日期 | 备注 |
|--------|------|--------|------|------|
| SNMP-001~011 | ☐ 未测 | | | |
| SNMP-B01~B05 | ☐ 未测 | | | |
| SNMP-E01~E02 | ☐ 未测 | | | |
| SNMP-P01 | ☐ 未测 | | | |

---

> 📝 下一步：[20-访客管理-预约管理测试](20-访客管理-预约管理测试.md)
