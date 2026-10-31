# warm-flow 1.8.8 集成设计（访客预约审批替换自研状态机）

> 状态：方案定版、技术事实已实证（2026-10-31）
> 分支约束：全部改动在 feature 分支；master/main 冻结不动（偏好 p1_ARFpBtZndcs）
> 环境约束：测试严格限定 192.168.1.60（应用）+ 192.168.1.55（MySQL/Redis）；禁止触碰生产 1.94.26.126（偏好 p1_ARIjNQXoLtc）

## 0. 决策记录

| 决策点 | 结论 | 依据 |
|---|---|---|
| 审批框架 | warm-flow 1.8.8 | 用户拍板；信创改造本轮不做 |
| 依赖坐标 | `org.dromara.warm:warm-flow-mybatis-sb4-starter:1.8.8` | 项目原生 MyBatis + Boot4，无 mybatis-plus |
| 流程模型 | 单级：发起 → 被访人审批 → 结束；能力预留多级/会签 | 维持现有业务语义 |
| 拒绝路径 | `TaskService.terminationByInsId(insId, params)` | 对齐 NetSight 已验证模式；1.8.x 禁止 REJECT 退回 START |
| 审批副作用 | 通行码/来访记录/短信保留在业务代码 `approveAppointment` 同事务执行 | NetSight 用 Spring 事件联动，非引擎监听器；同事务内联语义等价、风险最低 |
| 流程定义初始化 | 启动 Runner `defService.importJson(json)`（官方路径） | 引擎自建三表数据，避免手写 model_value 风险 |
| 建表方式 | 手动执行 DDL（**8 张表**；官方 7 表结构不完整，实测补 flow_form + 若干 create_by/update_by 列，见 §1.4） | **1.8.8 无自动建表**（BeanConfig 无 DDL 逻辑，WarmFlow 无 autoDdl 字段，已反编译实证）；表结构以 jar 内 Flow*Mapper.xml insert 列清单 + 1.55 实建为准 |
| 待办/审批人 | 发起时 `FlowParams.handler(发起人).nextHandler(审批人ID)`；审批前 `UPDATE flow_user SET processed_by=当前用户`（admin/园区管理员代批场景） | 对齐 NetSight 角色认领模式 |
| 配置 | `spring.main.allow-bean-definition-overriding: true` + `warm-flow.enabled: true` | NetSight（1.8.9）实证必需；1.8.8 同架构保险 |

## 1. 技术事实（已实证，勿重复勘察）

### 1.1 依赖树（中央仓库 1.8.8 全部核实）
```
org.dromara.warm:warm-flow-mybatis-sb4-starter:1.8.8
├── org.dromara.warm:warm-flow-plugin-json-jackson3:1.8.8   (JSON 插件，内嵌)
├── org.dromara.warm:warm-flow-mybatis-sb-starter:1.8.8
│   ├── org.dromara.warm:warm-flow-mybatis-core:1.8.8       (ORM 实体/DAO/Mapper)
│   ├── org.dromara.warm:warm-flow-plugin-modes-sb:1.8.8    (BeanConfig/自动配置)
│   ├── org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1  (与项目同版)
│   └── org.springframework:spring-tx
└── (排除 mybatis 版本冲突：sb4-starter 自带 4.0.1)
```
- `warm-flow-plugin-json:1.8.8` 是 **pom-only 占位**（无 jar），**不要引入**；JSON 功能在 `warm-flow-plugin-json-jackson3`
- `warm-flow-mybatis:1.8.8`、`warm-flow-mybatis-sb-starter:1.8.8`（jar 内嵌 pom 确认）

### 1.2 枚举权威值（javap 反编译 1.8.8 jar）
| 枚举 | 值 |
|---|---|
| NodeType | START=0, BETWEEN=1, END=2, SERIAL=3, PARALLEL=4, INCLUSIVE=5 |
| FlowStatus | TOBESUBMIT=0, APPROVAL=1, PASS=2, AUTO_PASS=3, TERMINATE=4, NULLIFY=5, CANCEL=6, RETRIEVE=7, FINISHED=8, REJECT=9, INVALID=10, TASK_BACK=11, RE_START=12, PENDING=13 |
| SkipType | PASS=PASS(审批通过), REJECT=REJECT(退回), NONE=NONE(无动作) |
| UserType | APPROVAL(1 审批人), TRANSFER(2 转办), DEPUTE(3 委派) |
| PublishStatus | EXPIRED, UNPUBLISHED, PUBLISHED（Integer key） |
| ActivityStatus | SUSPENDED, ACTIVITY（Integer key） |
| CooperateType | APPROVAL=1, TRANSFER=2, DEPUTE=3, COUNTERSIGN=4, VOTE=5, ADD_SIGNATURE=6, REDUCTION_SIGNATURE=7 |

### 1.3 核心 API（javap 确认，1.8.8）
- `FlowParams.build()` 链式：`flowCode/handler/nodeCode/permissionFlag(List<String>)/message/variable/skipType/flowStatus/hisStatus/activityStatus/cooperateType/ext/nextHandler(String...)/addHandlers/reductionHandlers/ignore(boolean)/ignoreDepute/ignoreCooperate/nextHandlerAppend/formData`
- `InsService.start(String flowCode, FlowParams)` — 发起
- `TaskService.skip(Long taskId, FlowParams)` — 审批跳转（**skipByInsId 已标记将删除，勿用**）
- `TaskService.terminationByInsId(Long insId, FlowParams)` — 终止/驳回
- `TaskService.getByInsId(Long insId)` — 按实例取待办任务
- `TaskService.getByIds(...)`、`revoke`、`transfer`、`depute`、`addSignature`、`reductionSignature`
- `DefService.getPublishByFlowCode(String)` / `importJson(String)` / `insertFlow(...)` / `publish(Long)` / `unPublish`
- `UserService.getByProcessedBys(...)` / `listByAssociatedAndTypes(...)` / `updatePermission(...)`
- `HisTaskService.getByInsId(Long)` — 历史轨迹
- 注解/事件：core 含 `Listener/GlobalListener/ListenerUtil`（审批副作用不依赖监听器，见决策记录）

### 1.4 表结构（8 表；含 2026-10-31 实测补丁）
> 列名 = 实体字段驼峰转下划线。类型以官方文档为准，实体独有字段按语义补型。
> **实测教训**：官方 table.html 的 7 表不完整——缺整张 **flow_form** 表（引擎 FlowFormMapper insert 用），且 flow_node/flow_skip 缺 create_by/update_by、flow_instance 缺 update_by（引擎 insert 报 `Unknown column`）。以下为 1.55 测试库实建结构（sql/warm-flow-1.8.8.sql 已按此回写）。

**flow_definition**：id BIGINT PK, flow_code VARCHAR(40) NN, flow_name VARCHAR(100) NN, model_value VARCHAR(40) NN DEFAULT 'CLASSICS', category VARCHAR(100), version VARCHAR(20) NN, is_publish BIT(1) NN DEFAULT 0, form_custom CHAR(1) DEFAULT 'N', form_path VARCHAR(100), activity_status BIT(1) NN DEFAULT 1, listener_type VARCHAR(100), listener_path VARCHAR(400), ext VARCHAR(500), create_time DATETIME, update_time DATETIME, create_by VARCHAR(64), update_by VARCHAR(64), del_flag CHAR(1) DEFAULT '0', tenant_id VARCHAR(40)
**flow_node**：id PK, node_type BIT(1) NN, definition_id BIGINT NN, node_code VARCHAR(100) NN, node_name VARCHAR(100), permission_flag VARCHAR(200), node_ratio DECIMAL(6,3), coordinate VARCHAR(100), any_node_skip VARCHAR(100), listener_type VARCHAR(100), listener_path VARCHAR(400), form_custom CHAR(1) DEFAULT 'N', form_path VARCHAR(100), version VARCHAR(20) NN, create_time DATETIME, update_time DATETIME, **create_by VARCHAR(64), update_by VARCHAR(64)（实测补）**, ext TEXT, del_flag CHAR(1) DEFAULT '0', tenant_id VARCHAR(40)
**flow_skip**：id PK, definition_id BIGINT NN, node_id BIGINT, now_node_code VARCHAR(100) NN, now_node_type BIT(1), next_node_code VARCHAR(100) NN, next_node_type BIT(1), skip_name VARCHAR(100), skip_type VARCHAR(40), skip_condition VARCHAR(200), coordinate VARCHAR(100), create_time DATETIME, update_time DATETIME, **create_by VARCHAR(64), update_by VARCHAR(64)（实测补）**, del_flag CHAR(1) DEFAULT '0', tenant_id VARCHAR(40)
**flow_instance**：id PK, definition_id BIGINT NN, flow_name VARCHAR(100), business_id VARCHAR(40) NN, node_type BIT(1) NN, node_code VARCHAR(40) NN, node_name VARCHAR(100), variable TEXT, flow_status VARCHAR(20) NN, activity_status BIT(1) NN DEFAULT 1, def_json TEXT, create_by VARCHAR(64), create_time DATETIME, update_time DATETIME, **update_by VARCHAR(64)（实测补）**, ext VARCHAR(500), del_flag CHAR(1) DEFAULT '0', tenant_id VARCHAR(40)
**flow_task**：id PK, definition_id BIGINT NN, instance_id BIGINT NN, flow_name VARCHAR(100), business_id VARCHAR(40), node_code VARCHAR(100) NN, node_name VARCHAR(100), node_type BIT(1) NN, flow_status VARCHAR(20) NN, form_custom CHAR(1) DEFAULT 'N', form_path VARCHAR(100), create_time DATETIME, update_time DATETIME, create_by VARCHAR(64), update_by VARCHAR(64), del_flag CHAR(1) DEFAULT '0', tenant_id VARCHAR(40)
**flow_his_task**：id PK, definition_id BIGINT NN, flow_name VARCHAR(100), instance_id BIGINT NN, task_id BIGINT NN, cooperate_type BIT(1) NN DEFAULT 0, business_id VARCHAR(40), node_code VARCHAR(100), node_name VARCHAR(100), node_type BIT(1), target_node_code VARCHAR(200), target_node_name VARCHAR(200), approver VARCHAR(40), collaborator VARCHAR(40), skip_type VARCHAR(10) NN, flow_status VARCHAR(20) NN, message VARCHAR(500), variable TEXT, ext TEXT, form_custom CHAR(1) DEFAULT 'N', form_path VARCHAR(100), create_time DATETIME, update_time DATETIME, del_flag CHAR(1) DEFAULT '0', tenant_id VARCHAR(40)
**flow_user**：id PK, type CHAR(1) NN, processed_by VARCHAR(80), associated BIGINT NN, create_time DATETIME, create_by VARCHAR(80), update_time DATETIME, update_by VARCHAR(64), del_flag CHAR(1) DEFAULT '0', tenant_id VARCHAR(40)
**flow_form**（实测补表）：id BIGINT PK, form_code VARCHAR(40), form_name VARCHAR(100), version VARCHAR(20), is_publish BIT(1) NN DEFAULT 0, form_type VARCHAR(40), form_path VARCHAR(100), form_content TEXT, ext VARCHAR(500), create_time DATETIME, update_time DATETIME, del_flag CHAR(1) DEFAULT '0', tenant_id VARCHAR(40), create_by VARCHAR(64), update_by VARCHAR(64)；KEY idx_ff_code(form_code)

索引建议：flow_definition(flow_code, is_publish, del_flag)；flow_node(definition_id, node_code)；flow_skip(definition_id, now_node_code)；flow_instance(business_id, del_flag)；flow_task(instance_id, del_flag)；flow_his_task(instance_id)；flow_user(associated, type, del_flag)。

### 1.5 配置（官方 config.html 实证）
```yaml
warm-flow:
  enabled: true
  banner: false            # 关闭启动 banner（可选）
  ui: false                # 本轮不接设计器 UI（P2：warm-flow-plugin-ui-sb-web）
  key-type: SnowId19       # 不填默认 orm 自带生成器/19位雪花
  data-source-type: mysql  # mybatis 包，默认 mysql
  logic-delete: true       # 逻辑删除（del_flag），默认值 2/0
  logic-delete-value: 2
  logic-not-delete-value: 0
```
> Boot4 必需：`spring.main.allow-bean-definition-overriding: true`（FlowAutoConfig extends BeanConfig）

### 1.6 官方文档
- 表结构：https://www.warm-flow.com/master/primary/table.html
- 配置 yml + 定义 json：https://warm-flow.dromara.org/master/primary/config.html
- 支持的功能/术语：https://warm-flow.dromara.org/master/primary/processterm.html
- 常见问题：https://www.warm-flow.com/master/other/troubleshooting.html

## 2. 代码改动清单（feature 分支，待执行）

1. **ruoyi-common/pom.xml（或根 pom dependencyManagement）**：加依赖
   ```xml
   <dependency>
       <groupId>org.dromara.warm</groupId>
       <artifactId>warm-flow-mybatis-sb4-starter</artifactId>
       <version>1.8.8</version>
   </dependency>
   ```
2. **ruoyi-admin/src/main/resources/application.yml**：加 `warm-flow:` 段 + `spring.main.allow-bean-definition-overriding: true`
3. **sql/ 新增 DDL**：`warm-flow-1.8.8.sql`（7 表，见 1.4）；测试库执行
4. **新增 `WarmFlowInitializer`（ApplicationRunner）**：`defService.getPublishByFlowCode("visitor_appointment")==null` 时 `importJson(...)` 建流程定义；流程 JSON 见 §3
5. **改造 `VisitorAppointmentServiceImpl.approveAppointment`**：
   - 状态校验（仅 PENDING 可审批）保留
   - 审批人身份校验保留（approverId 本人 / admin / smartparkAdmin）
   - 通过：`UPDATE flow_user SET processed_by=当前用户 WHERE associated=taskId AND type='APPROVAL'` → `taskService.skip(taskId, FlowParams.build().skipType(PASS).handler(当前用户).message(approveRemark).ignoreCooperate(true))` → 状态 APPROVED → 原副作用（通行码/来访记录/短信）
   - 拒绝：`taskService.terminationByInsId(insId, FlowParams.build().handler(当前用户).message(approveRemark))` → 状态 REJECTED（不发短信）
   - 状态机 `VisitorAppointmentState` 保留（业务状态），warm-flow 实例状态与业务状态同步映射：APPROVAL→PENDING、FINISHED/PASS→APPROVED、REJECT→REJECTED、TERMINATE→REJECTED
6. **新增预约 `insertAppointment`**：`insService.start("visitor_appointment", FlowParams.build().businessId(预约ID).handler(发起人).nextHandler(approverId).variable(map))` → 回填 warm-flow 实例 ID（可存预约表新列 flow_instance_id，或 ext 字段）
7. **待办查询**（GET /pending）：改查 warm-flow（按 flow_user.processed_by in (当前用户, admin/smartparkAdmin 角色) 取 taskId → instanceId → businessId 反查预约）或保留业务查询（approverId=当前用户）+ 以 warm-flow 状态过滤；**前端不动**（接口契约保持）
8. **回写一致性**：审批/取消/核验入口处同步业务状态与 warm-flow 实例状态（cancel→termination、checkin/complete 不改 warm-flow 状态或同步流转）

> 说明：业务状态机（VisitorAppointmentState）继续作为"业务权威状态"，warm-flow 作为"流程引擎状态"，二者在审批通过/拒绝/发起三处同步；取消/核验/完成维持业务状态机（不引入引擎状态，避免过度改造）。

## 3. 流程定义（visitor_appointment，importJson 用）
```json
{
  "flowCode": "visitor_appointment",
  "flowName": "访客预约审批",
  "category": "访客",
  "formCustom": "N",
  "version": "1.0",
  "nodeList": [
    {
      "nodeCode": "start", "nodeName": "发起", "nodeType": 0, "coordinate": "100,200|100,200",
      "skipList": [
        { "nowNodeCode": "start", "nextNodeCode": "approve", "skipName": "发起", "skipType": "PASS" }
      ]
    },
    {
      "nodeCode": "approve", "nodeName": "审批", "nodeType": 1, "coordinate": "300,200|300,200",
      "skipList": [
        { "nowNodeCode": "approve", "nextNodeCode": "end", "skipName": "通过", "skipType": "PASS" }
      ]
    },
    { "nodeCode": "end", "nodeName": "结束", "nodeType": 2, "coordinate": "500,200|500,200" }
  ]
}
```
- 拒绝不建 REJECT skip（对齐 NetSight：走 terminationByInsId）
- 审批节点 permission_flag 不写死（每个预约被访人不同），发起时 `nextHandler(approverId)` 动态指定办理人；代批走 flow_user 认领

## 4. 测试计划（192.168.1.60 + 192.168.1.55，禁止碰生产）
1. deploy.json test 段补全（ip=192.168.1.60、user/password=root/Chinaunicom@1358、backend.port=8080、backend.remoteDir/nginx 实测确认）
2. 1.55 测试库执行 warm-flow DDL（7 表）+ 确认连接账号（历史线索 sadmin/ry101-test 需实测，勿照抄）
3. 本地构建 mvn clean package -DskipTests + npm run build:prod（如需前端改动）
4. deploy.py --env test 部署（--check 先行）；后端重启走 start-topo.sh
5. 全链路验证：新增预约（PENDING→实例 APPROVAL）→ 被访人待办出现 → 通过（通行码生成/短信/来访记录）→ 拒绝（无短信/状态 REJECTED）→ 取消/核验闭环回归
6. 验证点：flow_instance/flow_task/flow_user/flow_his_task 数据一致性；页面审批列表按角色过滤回归

## 5. 信创预演（本轮不做，留档）
- warm-flow 为国产开源（dromara 社区）、Apache-2.0，无闭源依赖
- 信创关注点：数据库适配（warm-flow mybatis 包 data-source-type 支持 mysql/pg/oracle/dm 等，见官方文档）；中间件（内置 Tomcat 可换）；CPU 架构（Java 跨平台）
- 后续可评估：warm-flow-plugin-ui-sb-web 设计器（管理端流程配置入口）、多级审批、会签/票签（CooperateType 已支持）

## 6. 测试环境全链路验证结果（2026-10-31 实测通过）

### 6.1 权限校验根因（1.8.8，javap 实证）
- `TaskServiceImpl` 审批/终止统一权限校验：`perms = userService.getPermission(taskId, APPROVAL, TRANSFER, DEPUTE)`（取 flow_user.processed_by 集合），断言 `isNotEmpty(perms) && (isEmpty(FlowParams.permissionFlag) || permissionFlag 与 perms 有交集)`，否则抛"无法跳转到该节点,请检查当前用户是否有权限!"
- **因此 skip/终止必须带 `FlowParams.permissionFlag(当前操作人)`，且 flow_user.processed_by 已认领为当前操作人**（二者缺一即失败）
- `UserService.updatePermission(associated, users, type, delFlag, operator)` 第 4 参是 delFlag（true 才先删旧权限），传 false 仅追加不替换 → 代批认领必须走直接 SQL：`UPDATE flow_user SET processed_by=#{operator}, update_by=#{operator} WHERE associated=#{taskId} AND type='1' AND del_flag='0'`（Mapper `claimFlowUser`，ruoyi-system/VisitorAppointmentMapper）

### 6.2 修复内容（feature 分支）
1. VisitorAppointmentMapper.java/xml：新增 `claimFlowUser`（SQL 认领，替换原 updatePermission 追加逻辑）
2. VisitorAppointmentServiceImpl：
   - `ensureFlowUser` 改用 claimFlowUser 直接 SQL
   - `skipWarmFlow`：skip 的 FlowParams 补 `.permissionFlag(List.of(operator))`
   - `terminationWarmFlow`：终止前 getByInsId 取任务→认领→terminationByInsId 补 `.permissionFlag(...)`
   - `cancelAppointment`：取消同步终止前同样认领 + permissionFlag（失败仅 warn 不阻断）

### 6.3 全链路实测（HTTP + DB 断言，BASE=192.168.1.60:8080，admin/admin123）
| 链路 | 业务终态 | warm-flow 终态 | 证据 |
|---|---|---|---|
| 创建 → 审批通过（代批） | APPROVED + passCode=B1C764A5 + approveTime | instance node=end, **flow_status=8(FINISHED)** | flow_his_task: start→approve(PASS)、approve→end(PASS) |
| 创建 → 审批拒绝 | REJECTED（无通行码） | **flow_status=4(TERMINATE)** | his: approve→end, skip_type=PASS, flow_status=4 |
| 创建 → 取消 | CANCELLED | **flow_status=4(TERMINATE)** | his: approve→end, message=取消预约, flow_status=4 |
- flow_user：3 条均 processed_by=1（admin 代批认领生效）、update_by=1
- 预约表：9089/9090/9091 状态、通行码、审批时间、备注全部落库正确
- 全链路结论：**warm-flow 1.8.8 已完整替换自研状态机的流程推进/终止能力**（业务状态机保留为业务权威状态，二者在三处同步）

### 6.4 遗留与后续（P2，非本轮）
- 前端审批页未展示 warm-flow 流程状态（接口契约未变，前端零改动）
- 流程设计器 UI（warm-flow-plugin-ui-sb-web）未接入
- 信创预演（§5）本轮不做
