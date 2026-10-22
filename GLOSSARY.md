# 项目领域词汇表（GLOSSARY）

> 本文件为架构走查（improve-codebase-architecture）提供领域术语的规范命名，命名新深模块前先在此登记。
> 最后更新：2026-10-22（架构审查落地，任务#35）

## 访客预约（Visitor Appointment）

| 术语 | 定义 |
|---|---|
| 预约（Appointment） | 访客来访前提交的预约单，落在 `visitor_appointment` 表。状态机见「预约状态」。 |
| 预约状态机（VisitorAppointmentState） | `ruoyi-system/.../domain/VisitorAppointmentState.java`。状态定义与合法转移的唯一权威 module：`can()` 校验、`transition()` 转移。Service 各方法不得再手写状态判断。 |
| 预约事件（Event） | 状态机的转移事件：APPROVE（审批通过）、REJECT（审批拒绝）、CANCEL（取消）、CHECKIN（到访确认）、COMPLETE（完成离开）。 |
| 来访记录（VisitorLog） | 实际到访/登记记录，落在 `visitor_log` 表。预约审批通过或现场登记时由记录工厂生成。 |
| 记录工厂（VisitorLogFactory） | `ruoyi-system/.../domain/VisitorLogFactory.java`。appointment→log 字段映射的唯一入口，消灭重复拷贝。 |
| 通行码（passCode） | 访客通行凭证（8 位大写），预约生成后用于保安核验与到访确认。 |
| 被访人匹配（assignApproverAndDept） | hostName → 审批人(userId)+部门(deptId) 的三级匹配（hostAccount→loginName→nickName），失败即拒绝创建。 |

## 通行核验（Pass）

| 术语 | 定义 |
|---|---|
| 通行信息（PassInfoDTO） | `ruoyi-system/.../domain/vo/PassInfoDTO.java`。保安核验/通行展示的统一契约，预约与来访记录两来源归一。 |
| 到访确认（checkin） | 显式写操作：`POST /pass/{code}/checkin`，状态 → VISITING。`GET /pass/{code}` 为纯读，不带任何副作用。 |

## 短信（SMS）

| 术语 | 定义 |
|---|---|
| 发送计划（SmsPlan） | `ruoyi-system/.../sms/SmsPlan.java`。SmsResolver 解析 biz_code 的产物：业务绑定+签名模板+渠道+驱动。 |
| 解析器（SmsResolver） | `ruoyi-system/.../sms/SmsResolver.java`。「biz→签名模板→渠道→驱动」解析链的唯一实现，即时/定时/重发共用；驱动解析与缓存收敛于此。 |
| 短信驱动（SmsDriver） | 厂商适配 seam：ALIYUN / TENCENT(stub) / API(GenericHttpDriver)。新厂商 = 在 SmsResolver.resolveDriver 注册新 adapter。 |
| 业务编码（bizCode） | `sys_sms_biz.biz_code`，如 visitor_pending_notify / visitor_approval，模板与渠道在数据库中绑定。 |

## 数据范围（多租户）

| 术语 | 定义 |
|---|---|
| 组织过滤（DeptScopeInterceptor） | ruoyi-framework MyBatis 拦截器，按主表白名单 + dept_id（本部门+子部门）改写 SELECT。已知脆弱（字符串解析），见架构候选3。 |
